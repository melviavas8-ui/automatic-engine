package daamky.ua.api.settings.impl;

import daamky.gui.UiStyle;

/** Client "Style" switch: Legacy or Liberty City. Stored in system/style.daamky, not in module configs. */
public final class StyleSetting extends ModeSetting {
    public StyleSetting() {
        super("Style", "Look of the client: Legacy or Liberty City.", false, UiStyle.current(), UiStyle.NAMES.toArray(String[]::new));
        this.addListener((setting, oldValue, newValue) -> UiStyle.select(newValue));
    }

    @Override
    protected String normalize(String value) {
        return UiStyle.find(value);
    }

    @Override
    public boolean isPersistent() {
        return false;
    }
}
