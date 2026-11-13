package daamky.ua.api.settings.impl;

import daamky.gui.GuiStyle;

/** ClickGUI "Type" switch: Window or DropDown. Stored in system/gui.daamky, not in module configs. */
public final class GuiTypeSetting extends ModeSetting {
    public GuiTypeSetting() {
        super("Type", "ClickGUI style: Window or DropDown.", false, GuiStyle.current(), GuiStyle.NAMES.toArray(String[]::new));
        this.addListener((setting, oldValue, newValue) -> GuiStyle.select(newValue));
    }

    @Override
    protected String normalize(String value) {
        return GuiStyle.find(value);
    }

    @Override
    public boolean isPersistent() {
        return false;
    }
}
