package me.desht.modularrouters.item.module;

import me.desht.modularrouters.block.tile.TileEntityItemRouter;
import me.desht.modularrouters.gui.module.GuiModule;
import me.desht.modularrouters.gui.module.GuiModuleDetector;
import me.desht.modularrouters.item.ModItems;
import me.desht.modularrouters.logic.compiled.CompiledDetectorModule;
import me.desht.modularrouters.logic.compiled.CompiledModule;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import java.util.List;

public class DetectorModule extends Module {
    public enum SignalType {
        NONE, WEAK, STRONG;

        public static SignalType getType(boolean strong) {
            return strong ? STRONG : WEAK;
        }
    }


    @Override
    public CompiledModule compile(TileEntityItemRouter tileEntityItemRouter, ItemStack stack) {
        return new CompiledDetectorModule(tileEntityItemRouter, stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addExtraInformation(ItemStack itemstack, EntityPlayer player, List<String> list, boolean par4) {
        super.addExtraInformation(itemstack, player, list, par4);
        CompiledDetectorModule ds = new CompiledDetectorModule(null, itemstack);
        list.add(I18n.format("itemText.misc.redstoneLevel",
                ds.getSignalLevel(), I18n.format("itemText.misc.strongSignal." + ds.isStrongSignal())));
    }

    @Override
    public Class<? extends GuiModule> getGuiHandler() {
        return GuiModuleDetector.class;
    }

    @Override
    public IRecipe getRecipe() {
        return new ShapelessOreRecipe(ItemModule.makeItemStack(ItemModule.ModuleType.DETECTOR),
                ModItems.blankModule, Items.COMPARATOR);
    }

    @Override
    public boolean canBeRegulated() {
        return false;
    }
}
