package me.desht.modularrouters.logic.compiled;

import me.desht.modularrouters.block.tile.TileEntityItemRouter;
import me.desht.modularrouters.util.ModuleHelper;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class CompiledDropperModule extends CompiledModule {
    private final int pickupDelay;

    public CompiledDropperModule(TileEntityItemRouter router, ItemStack stack) {
        super(router, stack);

        pickupDelay = ModuleHelper.getPickupDelay(stack);
    }

    @Override
    public boolean execute(TileEntityItemRouter router) {
        ItemStack stack = router.getBufferItemStack();
        if (stack != null && getFilter().test(stack) && isRegulationOK(router, false)) {
            int nItems = Math.min(router.getItemsPerTick(), stack.stackSize - getRegulationAmount());
            if (nItems < 0) {
                return false;
            }
            ItemStack toDrop = router.peekBuffer(nItems);
            BlockPos pos = getTarget().pos;
            EnumFacing face = getTarget().face;
            EntityItem item = new EntityItem(router.getWorld(),
                    pos.getX() + 0.5 + 0.2 * face.getFrontOffsetX(),
                    pos.getY() + 0.5 + 0.2 * face.getFrontOffsetY(),
                    pos.getZ() + 0.5 + 0.2 * face.getFrontOffsetZ(),
                    toDrop);
            item.setPickupDelay(pickupDelay);
            setupItemVelocity(router, item);
            if (router.getWorld().spawnEntityInWorld(item)) {
                router.extractBuffer(toDrop.stackSize);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    protected void setupItemVelocity(TileEntityItemRouter router, EntityItem item) {
        item.motionX = item.motionY = item.motionZ = 0.0;
    }
}
