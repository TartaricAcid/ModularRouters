package me.desht.modularrouters.util;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import me.desht.modularrouters.logic.filter.Filter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.items.IItemHandler;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BlockUtil {
    private static final String[] REED_ITEM = new String[]{"block", "field_150935_a", "a"};

    private static IBlockState getPlaceableState(ItemStack stack, World world, BlockPos pos, EnumFacing facing) {
        // With thanks to Vazkii for inspiration from the Rannuncarpus code :)
        Item item = stack.getItem();
        IBlockState res = null;
        if (item instanceof ItemBlock) {
            res = ((ItemBlock) item).block.getStateFromMeta(item.getMetadata(stack.getItemDamage()));
        } else if (item instanceof ItemBlockSpecial) {
            res = ((Block) ReflectionHelper.getPrivateValue(ItemBlockSpecial.class, (ItemBlockSpecial) item, REED_ITEM)).getDefaultState();
        } else if (item instanceof ItemRedstone) {
            res = Blocks.REDSTONE_WIRE.getDefaultState();
        } else if (item instanceof IPlantable) {
            IBlockState state = ((IPlantable) item).getPlant(world, pos);
            res = ((state.getBlock() instanceof BlockCrops) && ((BlockCrops) state.getBlock()).canBlockStay(world, pos, state)) ? state : null;
        } else if (item instanceof ItemSkull) {
            res = Blocks.SKULL.getDefaultState();
            // try to place skull on horizontal surface below if possible
            BlockPos pos2 = pos.down();
            if (world.getBlockState(pos2).isSideSolid(world, pos2, EnumFacing.UP)) {
                facing = EnumFacing.UP;
            }
        }
        if (res != null && res.getProperties().containsKey(BlockDirectional.FACING)) {
            res = res.withProperty(BlockDirectional.FACING, facing);
        }
        return res;
    }

    private static void handleSkullPlacement(World worldIn, BlockPos pos, ItemStack stack, EnumFacing facing) {
        // adapted from ItemSkull#onItemUse()

        int i = 0;
        if (worldIn.getBlockState(pos).getValue(BlockDirectional.FACING) == EnumFacing.UP) {
            i = MathHelper.floor_double((double) (facing.getOpposite().getHorizontalAngle() * 16.0F / 360.0F) + 0.5D) & 15;
        }

        TileEntity tileentity = worldIn.getTileEntity(pos);
        if (tileentity instanceof TileEntitySkull) {
            TileEntitySkull tileentityskull = (TileEntitySkull) tileentity;
            if (stack.getMetadata() == 3) {   // player head
                GameProfile gameprofile = null;
                if (stack.hasTagCompound()) {
                    NBTTagCompound nbttagcompound = stack.getTagCompound();
                    if (nbttagcompound.hasKey("SkullOwner", Constants.NBT.TAG_COMPOUND)) {
                        gameprofile = NBTUtil.readGameProfileFromNBT(nbttagcompound.getCompoundTag("SkullOwner"));
                    } else if (nbttagcompound.hasKey("SkullOwner", Constants.NBT.TAG_STRING) && !nbttagcompound.getString("SkullOwner").isEmpty()) {
                        gameprofile = new GameProfile(null, nbttagcompound.getString("SkullOwner"));
                    }
                }
                tileentityskull.setPlayerProfile(gameprofile);
            } else {
                tileentityskull.setType(stack.getMetadata());
            }

            tileentityskull.setSkullRotation(i);
            Blocks.SKULL.checkWitherSpawn(worldIn, pos, tileentityskull);
        }
    }

    /**
     * Try to place the given item as a block in the world.  This will fail if the block currently at the
     * placement position isn't replaceable, or world physics disallows the new block from being placed.
     *
     * @param toPlace item to place
     * @param world   the world
     * @param pos     position in the world to place at
     * @return the new block state if successful, null otherwise
     */
    public static IBlockState tryPlaceAsBlock(ItemStack toPlace, World world, BlockPos pos, EnumFacing facing) {
        IBlockState currentState = world.getBlockState(pos);
        if (!currentState.getBlock().isReplaceable(world, pos)) {
            return null;
        }

        IBlockState newState = getPlaceableState(toPlace, world, pos, facing);
        if (newState != null && newState.getBlock().canPlaceBlockAt(world, pos)) {
            EntityPlayer fakePlayer = FakePlayer.getFakePlayer((WorldServer) world, pos).get();
            if (fakePlayer == null) {
                return null;
            }
            BlockSnapshot snap = new BlockSnapshot(world, pos, newState);
            BlockEvent.PlaceEvent event = new BlockEvent.PlaceEvent(snap, null, fakePlayer);
            MinecraftForge.EVENT_BUS.post(event);
            if (!event.isCanceled() && world.setBlockState(pos, newState)) {
                newState.getBlock().onBlockPlacedBy(world, pos, newState, fakePlayer, toPlace);
                if (newState.getBlock() == Blocks.SKULL) {
                    handleSkullPlacement(world, pos, toPlace, facing);
                }
                return newState;
            }
        }

        return null;
    }

    /**
     * Try to break the block at the given position. If the block has any drops, but no drops pass the filter, then the
     * block will not be broken. Liquid, air & unbreakable blocks (bedrock etc.) will never be broken.  Drops will be
     * available via the DropResult object, organised by whether or not they passed the filter.
     *
     * @param world     the world
     * @param pos       the block position
     * @param filter    filter for the block's drops
     * @param silkTouch use silk touch when breaking the block
     * @param fortune   use fortune when breaking the block
     * @return a drop result object
     */
    public static BreakResult tryBreakBlock(World world, BlockPos pos, Filter filter, boolean silkTouch, int fortune) {
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (block.isAir(state, world, pos) || state.getBlockHardness(world, pos) < 0 || block instanceof BlockLiquid) {
            return BreakResult.NOT_BROKEN;
        }

        EntityPlayer fakePlayer = FakePlayer.getFakePlayer((WorldServer) world, pos).get();
        List<ItemStack> allDrops = getDrops(world, pos, fakePlayer, silkTouch, fortune);

        Map<Boolean, List<ItemStack>> groups = allDrops.stream().collect(Collectors.partitioningBy(filter));
        if (allDrops.isEmpty() || !groups.get(true).isEmpty()) {
            BlockEvent.BreakEvent breakEvent = new BlockEvent.BreakEvent(world, pos, state, fakePlayer);
            MinecraftForge.EVENT_BUS.post(breakEvent);
            if (!breakEvent.isCanceled()) {
                world.setBlockToAir(pos);
                return new BreakResult(true, groups);
            }
        }
        return BreakResult.NOT_BROKEN;
    }

    private static List<ItemStack> getDrops(World world, BlockPos pos, EntityPlayer player, boolean silkTouch, int fortune) {
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        if (silkTouch) {
            Item item = Item.getItemFromBlock(block);
            if (item == null) {
                return Collections.emptyList();
            } else {
                return Lists.newArrayList(new ItemStack(item, 1, block.getMetaFromState(state)));
            }
        }

        List<ItemStack> drops = block.getDrops(world, pos, state, fortune);
        float dropChance = ForgeEventFactory.fireBlockHarvesting(drops, world, pos, state, fortune, 1.0F, false, player);

        return drops.stream().filter(s -> world.rand.nextFloat() <= dropChance).collect(Collectors.toList());
    }

    public static String getBlockName(World w, BlockPos pos) {
        if (w == null) {
            return null;
        }
        IBlockState state = w.getBlockState(pos);
        if (state.getBlock().isAir(state, w, pos)) {
            return "";
        } else {
            ItemStack stack = state.getBlock().getItem(w, pos, state);
            if (stack != null) {
                return stack.getDisplayName();
            } else {
                return state.getBlock().getLocalizedName();
            }
        }
    }

    public static class BreakResult {
        static final BreakResult NOT_BROKEN = new BreakResult(false, Collections.emptyMap());

        private final boolean blockBroken;
        private final Map<Boolean, List<ItemStack>> drops;

        BreakResult(boolean blockBroken, Map<Boolean, List<ItemStack>> drops) {
            this.blockBroken = blockBroken;
            this.drops = drops;
        }

        public boolean isBlockBroken() {
            return blockBroken;
        }

        List<ItemStack> getFilteredDrops(boolean passed) {
            return drops.getOrDefault(passed, Collections.emptyList());
        }

        public void processDrops(World world, BlockPos pos, IItemHandler handler) {
            for (ItemStack drop : getFilteredDrops(true)) {
                ItemStack excess = handler.insertItem(0, drop, false);
                if (excess != null) {
                    InventoryUtils.dropItems(world, pos, excess);
                }
            }
            for (ItemStack drop : getFilteredDrops(false)) {
                InventoryUtils.dropItems(world, pos, drop);
            }
        }
    }
}
