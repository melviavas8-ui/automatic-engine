package daamky.ua.api.settings.impl;

import daamky.gui.GuiStyle;

/** ClickGUI "Type" switch: Window or DropDown (plus CS in Liberty City). Stored in system/gui.daamky, not in module configs. */
public final class GuiTypeSetting extends ModeSetting {
    public GuiTypeSetting() {
        super("Type", "ClickGUI type for the selected style.", false, GuiStyle.current(), GuiStyle.names().toArray(String[]::new));
        this.addListener((setting, oldValue, newValue) -> GuiStyle.select(newValue));
    }

    /** Called after the client style changed: swaps the list to the new style's GUI types and keeps its own selected type. */
    public void refreshForStyle() {
        String wanted = GuiStyle.current();
        this.replaceModes(GuiStyle.names().toArray(String[]::new));
        this.setValue(wanted);
        GuiStyle.select(wanted);
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
