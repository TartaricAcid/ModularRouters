package me.desht.modularrouters.logic.compiled;

import me.desht.modularrouters.block.tile.TileEntityItemRouter;
import me.desht.modularrouters.config.Config;
import me.desht.modularrouters.item.module.ExtruderModule;
import me.desht.modularrouters.util.BlockUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.IFluidBlock;

public class CompiledExtruderModule extends CompiledModule {
    private static final String NBT_EXTRUDER_DIST = "ExtruderDist";

    private final boolean silkTouch;
    private int distance;  // marks the current extension length (0 = no extrusion)

    public CompiledExtruderModule(TileEntityItemRouter router, ItemStack stack) {
        super(router, stack);
        distance = router.getExtData().getInteger(NBT_EXTRUDER_DIST + getFacing());
        silkTouch = EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, stack) > 0;
    }

    @Override
    public boolean execute(TileEntityItemRouter router) {
        boolean extend = shouldExtend(router);
        World world = router.getWorld();

        if (extend && !router.isBufferEmpty() && distance < ExtruderModule.maxDistance(router) && isRegulationOK(router, false)) {
            // try to extend
            BlockPos placePos = router.getPos().offset(getFacing(), distance + 1);
            ItemStack toPlace = router.peekBuffer(1);
            IBlockState state = BlockUtil.tryPlaceAsBlock(toPlace, world, placePos, getFacing());
            if (state != null) {
                router.extractBuffer(1);
                router.getExtData().setInteger(NBT_EXTRUDER_DIST + getFacing(), ++distance);
                if (Config.extruderSound) {
                    router.playSound(null, placePos,
                            state.getBlock().getSoundType(state, world, placePos, null).getPlaceSound(),
                            SoundCategory.BLOCKS, 1.0f, 0.5f + distance * 0.1f);
                }
                return true;
            }
        } else if (!extend && distance > 0 && isRegulationOK(router, true)) {
            // try to retract
            BlockPos breakPos = router.getPos().offset(getFacing(), distance);
            IBlockState oldState = world.getBlockState(breakPos);
            Block oldBlock = oldState.getBlock();
            if (oldBlock.isAir(oldState, world, breakPos) || oldBlock instanceof BlockLiquid || oldBlock instanceof IFluidBlock) {
                // nothing there? continue to retract anyway...
                router.getExtData().setInteger(NBT_EXTRUDER_DIST + getFacing(), --distance);
                return false;
            }
            BlockUtil.BreakResult breakResult = BlockUtil.tryBreakBlock(world, breakPos, getFilter(), silkTouch, 0);
            if (breakResult.isBlockBroken()) {
                router.getExtData().setInteger(NBT_EXTRUDER_DIST + getFacing(), --distance);
                breakResult.processDrops(world, breakPos, router.getBuffer());
                if (Config.extruderSound) {
                    router.playSound(null, breakPos,
                            oldBlock.getSoundType(oldState, world, breakPos, null).getBreakSound(),
                            SoundCategory.BLOCKS, 1.0f, 0.5f + distance * 0.1f);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean shouldRun(boolean powered, boolean pulsed) {
        return true;
    }

    private boolean shouldExtend(TileEntityItemRouter router) {
        switch (getRedstoneBehaviour()) {
            case ALWAYS:
                return router.getRedstonePower() > 0;
            case HIGH:
                return router.getRedstonePower() == 15;
            case LOW:
                return router.getRedstonePower() == 0;
            default:
                return false;
        }
    }
}
