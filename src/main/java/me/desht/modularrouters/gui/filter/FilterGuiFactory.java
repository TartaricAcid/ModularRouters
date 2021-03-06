package me.desht.modularrouters.gui.filter;

import me.desht.modularrouters.ModularRouters;
import me.desht.modularrouters.block.tile.TileEntityItemRouter;
import me.desht.modularrouters.container.ContainerSmartFilter;
import me.desht.modularrouters.container.FilterHandler;
import me.desht.modularrouters.container.FilterHandler.ModuleFilterHandler;
import me.desht.modularrouters.item.module.ItemModule;
import me.desht.modularrouters.item.smartfilter.ItemSmartFilter;
import me.desht.modularrouters.item.smartfilter.SmartFilter;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class FilterGuiFactory {
    public static GuiScreen createGui(EntityPlayer player, EnumHand hand) {
        return createGui(player, player.getHeldItem(hand), null, -1, -1, hand);
    }

    public static GuiScreen createGui(EntityPlayer player, World world, int x, int y, int z) {
        TileEntityItemRouter router = TileEntityItemRouter.getRouterAt(world, new BlockPos(x, y, z));
        if (router != null) {
            int moduleIndex = router.getModuleConfigSlot(player);
            int filterIndex = router.getFilterConfigSlot(player);
            ItemStack filterStack = getFilterStackInInstalledModule(router, moduleIndex, filterIndex);
            if (ItemSmartFilter.getFilter(filterStack) != null) {
                router.clearConfigSlot(player);
                return FilterGuiFactory.createGui(player, filterStack, router.getPos(), moduleIndex, filterIndex, null);
            }
        }
        return null;
    }

    private static GuiScreen createGui(EntityPlayer player, ItemStack heldStack, BlockPos routerPos, int moduleSlotIndex, int filterSlotIndex, EnumHand hand) {
        ItemStack filterStack;
        if (ItemModule.getModule(heldStack) != null) {
            filterSlotIndex = ItemModule.getFilterConfigSlot(heldStack);
            filterStack = getFilterStackInUninstalledModule(heldStack, filterSlotIndex);
            ItemModule.setFilterConfigSlot(heldStack, -1);
        } else if (ItemSmartFilter.getFilter(heldStack) != null) {
            filterStack = heldStack;
        } else {
            return null;
        }

        if (filterStack == null) {
            return null;
        }

        try {
            SmartFilter filter = ItemSmartFilter.getFilter(filterStack);
            Class<? extends GuiScreen> clazz = filter.getGuiHandler();
            if (filter.hasGuiContainer()) {
                Constructor<? extends GuiScreen> ctor = clazz.getConstructor(ContainerSmartFilter.class, BlockPos.class, Integer.class, Integer.class, EnumHand.class);
                TileEntityItemRouter router = routerPos == null ? null : TileEntityItemRouter.getRouterAt(player.getEntityWorld(), routerPos);
                return ctor.newInstance(createContainer(player, filterStack, hand, router), routerPos, moduleSlotIndex, filterSlotIndex, hand);
            } else {
                Constructor<? extends GuiScreen> ctor = clazz.getConstructor(ItemStack.class, BlockPos.class, Integer.class, Integer.class, EnumHand.class);
                return ctor.newInstance(filterStack, routerPos, moduleSlotIndex, filterSlotIndex, hand);
            }
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Create a container for a filter held in the player's hand (or in a module held in a player's hand)
     *
     * @param player the player
     * @param hand hand in which the item is held
     * @return the container object
     */
    public static Container createContainer(EntityPlayer player, EnumHand hand) {
        ItemStack heldStack = player.getHeldItem(hand);
        if (ItemModule.getModule(heldStack) != null) {
            // filter is in a module in player's hand
            int filterIndex = ItemModule.getFilterConfigSlot(heldStack);
            if (filterIndex >= 0) {
                ItemStack filterStack = getFilterStackInUninstalledModule(heldStack, filterIndex);
                ItemModule.setFilterConfigSlot(heldStack, -1);
                return createContainer(player, filterStack, hand, null);
            }
        } else {
            // filter is held directly in player's hand
            return createContainer(player, heldStack, hand, null);
        }
        return null;
    }

    /**
     * Create a container for a filter in a module which is installed in a router.
     *
     * @param player the player
     * @param world world the router is in
     * @param x router's X position
     * @param y router's Y position
     * @param z router's Z position
     * @return the container object
     */
    public static Container createContainer(EntityPlayer player, World world, int x, int y, int z) {
        TileEntityItemRouter router = TileEntityItemRouter.getRouterAt(world, new BlockPos(x, y, z));
        if (router != null) {
            int moduleIndex = router.getModuleConfigSlot(player);
            int filterIndex = router.getFilterConfigSlot(player);
            ItemStack filterStack = getFilterStackInInstalledModule(router, moduleIndex, filterIndex);
            return createContainer(player, filterStack, null, router);
        }
        return null;
    }

    private static Container createContainer(EntityPlayer player, ItemStack stack, EnumHand hand, TileEntityItemRouter router) {
        SmartFilter f = ItemSmartFilter.getFilter(stack);
        return f != null && f.hasGuiContainer() ? f.createContainer(player, stack, hand, router) : null;
    }

    private static ItemStack getFilterStackInUninstalledModule(ItemStack moduleStack, int filterIdx) {
        return getStackInSlot(moduleStack, filterIdx);
    }

    private static ItemStack getFilterStackInInstalledModule(TileEntityItemRouter router, int moduleIdx, int filterIdx) {
        if (moduleIdx < 0 || filterIdx < 0) {
            return null;
        }
        ItemStack installedModuleStack = router.getModules().getStackInSlot(moduleIdx);
        return getStackInSlot(installedModuleStack, filterIdx);
    }

    private static ItemStack getStackInSlot(ItemStack moduleStack, int filterIdx) {
        FilterHandler handler = new ModuleFilterHandler(moduleStack);
        ItemStack stack = handler.getStackInSlot(filterIdx);
        if (stack != null) {
            return stack;
        } else {
            ModularRouters.logger.warn("can't find filter item in module '" + moduleStack + "', slot " + filterIdx);
            return null;
        }
    }
}
