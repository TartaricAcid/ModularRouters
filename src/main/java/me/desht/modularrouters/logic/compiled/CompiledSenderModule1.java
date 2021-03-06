package me.desht.modularrouters.logic.compiled;

import me.desht.modularrouters.ModularRouters;
import me.desht.modularrouters.block.tile.TileEntityItemRouter;
import me.desht.modularrouters.config.Config;
import me.desht.modularrouters.item.module.SenderModule1;
import me.desht.modularrouters.item.upgrade.ItemUpgrade;
import me.desht.modularrouters.logic.ModuleTarget;
import me.desht.modularrouters.network.ParticleBeamMessage;
import me.desht.modularrouters.util.BlockUtil;
import me.desht.modularrouters.util.InventoryUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.items.IItemHandler;

import java.awt.*;

public class CompiledSenderModule1 extends CompiledModule {
    private static final Color particleColor = Color.ORANGE;

    public CompiledSenderModule1(TileEntityItemRouter router, ItemStack stack) {
        super(router, stack);
    }

    @Override
    public boolean execute(TileEntityItemRouter router) {
        IItemHandler buffer = router.getBuffer();
        ItemStack bufferStack = buffer.getStackInSlot(0);
        if (bufferStack != null && getFilter().test(bufferStack)) {
            PositionedItemHandler target = findTargetInventory(router);
            if (target != null) {
                int nToSend = router.getItemsPerTick();
                if (getRegulationAmount() > 0) {
                    int existing = InventoryUtils.countItems(bufferStack, target.handler, getRegulationAmount(), !getFilter().getFlags().isIgnoreMeta());
                    nToSend = Math.min(nToSend, getRegulationAmount() - existing);
                    if (nToSend <= 0) {
                        return false;
                    }
                }
                int sent = InventoryUtils.transferItems(buffer, target.handler, 0, nToSend);
                if (sent > 0) {
                    if (Config.senderParticles) {
                        playParticles(router, target.pos);
                    }
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    protected void playParticles(TileEntityItemRouter router, BlockPos targetPos) {
        if (router.getUpgradeCount(ItemUpgrade.UpgradeType.MUFFLER) < 2) {
            Vec3d vec1 = new Vec3d(router.getPos()).addVector(0.5, 0.5, 0.5);
            Vec3d vec2 = new Vec3d(targetPos).addVector(0.5, 0.5, 0.5);
            NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(router.getWorld().provider.getDimension(), vec1.xCoord, vec1.yCoord, vec1.zCoord, 32);
            ModularRouters.network.sendToAllAround(new ParticleBeamMessage(vec1.xCoord, vec1.yCoord, vec1.zCoord, vec2.xCoord, vec2.yCoord, vec2.zCoord, particleColor, 0.3f), point);
        }
    }

    protected PositionedItemHandler findTargetInventory(TileEntityItemRouter router) {
        ModuleTarget target = getActualTarget(router);
        if (target != null) {
            IItemHandler handler = InventoryUtils.getInventory(DimensionManager.getWorld(target.dimId), target.pos, target.face);
            return handler == null ? null : new PositionedItemHandler(target.pos, handler);
        }
        return null;
    }

    @Override
    public ModuleTarget getActualTarget(TileEntityItemRouter router) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(getTarget().pos);
        EnumFacing face = getTarget().face;
        World world = router.getWorld();
        int max = SenderModule1.maxDistance(router);
        for (int i = 1; i < max; i++) {
            if (world.getTileEntity(pos) != null) {
                return new ModuleTarget(world.provider.getDimension(), pos.toImmutable(), face, BlockUtil.getBlockName(world, pos));
            } else if (!isPassable(world, pos, face)) {
                return null;
            }
            pos.move(getFacing());
        }
        return null;
    }

    private boolean isPassable(World w, BlockPos pos, EnumFacing face) {
        IBlockState state = w.getBlockState(pos);
        return !state.getBlock().isBlockSolid(w, pos, face) || !state.isOpaqueCube();
    }

    static class PositionedItemHandler {
        private final BlockPos pos;
        private final IItemHandler handler;

        PositionedItemHandler(BlockPos pos, IItemHandler handler) {
            this.pos = pos;
            this.handler = handler;
        }
    }
}
