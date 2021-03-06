package me.desht.modularrouters.item.module;

import me.desht.modularrouters.block.tile.TileEntityItemRouter;
import me.desht.modularrouters.gui.module.GuiModule;
import me.desht.modularrouters.gui.module.GuiModulePlayer;
import me.desht.modularrouters.logic.compiled.CompiledModule;
import me.desht.modularrouters.logic.compiled.CompiledPlayerModule;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;

import java.util.List;

public class PlayerModule extends Module {
    @Override
    protected void addExtraInformation(ItemStack stack, EntityPlayer player, List<String> list, boolean par4) {
        super.addExtraInformation(stack, player, list, par4);
        CompiledPlayerModule cpm = new CompiledPlayerModule(null, stack);
        list.add(TextFormatting.YELLOW + I18n.format("itemText.security.owner", cpm.getPlayerName()));
        list.add(TextFormatting.YELLOW + String.format(TextFormatting.YELLOW + "%s: " + TextFormatting.AQUA + "%s %s %s",
                I18n.format("itemText.misc.operation"),
                I18n.format("tile.itemRouter.name"),
                cpm.getOperation().getSymbol(),
                I18n.format("guiText.label.playerSect." + cpm.getSection())));
    }

    @Override
    public CompiledModule compile(TileEntityItemRouter router, ItemStack stack) {
        return new CompiledPlayerModule(router, stack);
    }

    @Override
    public Class<? extends GuiModule> getGuiHandler() {
        return GuiModulePlayer.class;
    }

    @Override
    public boolean isDirectional() {
        return false;
    }

    @Override
    public IRecipe getRecipe() {
        return new ShapedOreRecipe(ItemModule.makeItemStack(ItemModule.ModuleType.PLAYER),
                " h ", "szp", " c ",
                'h', Items.DIAMOND_HELMET,
                's', ItemModule.makeItemStack(ItemModule.ModuleType.SENDER3),
                'z', new ItemStack(Items.SKULL, 1, OreDictionary.WILDCARD_VALUE),
                'p', ItemModule.makeItemStack(ItemModule.ModuleType.PULLER),
                'c', Items.DIAMOND_CHESTPLATE);
    }

    @Override
    public EnumActionResult onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing face, float x, float y, float z) {
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        } else if (player.isSneaking()) {
            ItemModule.setOwner(stack, player);
            player.addChatMessage(new TextComponentTranslation("itemText.security.owner", player.getDisplayNameString()));
            return EnumActionResult.SUCCESS;
        } else {
            return super.onItemUse(stack, player, world, pos, hand, face, x, y, z);
        }
    }
}
