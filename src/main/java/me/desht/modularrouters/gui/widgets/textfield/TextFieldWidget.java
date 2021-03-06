package me.desht.modularrouters.gui.widgets.textfield;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;

public class TextFieldWidget extends GuiTextField {
    private final TextFieldManager manager;
    private final int ordinal;  // order in which this field appears in the textfield manager

    public TextFieldWidget(TextFieldManager manager, int componentId, FontRenderer fontrendererObj, int x, int y, int par5Width, int par6Height) {
        super(componentId, fontrendererObj, x, y, par5Width, par6Height);
        this.manager = manager;
        this.ordinal = manager.addTextField(this);
    }

    @Override
    public void setFocused(boolean isFocusedIn) {
        super.setFocused(isFocusedIn);
        manager.onTextFieldFocusChange(ordinal, isFocusedIn);
    }

    /**
     * Convenience method
     */
    public void useGuiTextBackground() {
        setTextColor(0xffffffff);
        setDisabledTextColour(0xffffffff);
        setEnableBackgroundDrawing(false);
    }

    public void onMouseWheel(int direction) {}

    /**
     * Get the order in which this field appears in the text field manager.
     *
     * @return the order
     */
    public int getOrdinal() {
        return ordinal;
    }
}
