package daamky.ua.api.events.impl;

import com.mojang.blaze3d.platform.InputConstants.Type;
import daamky.ua.api.events.Event;
import net.minecraft.client.gui.screens.Screen;

public record KeyEvent(Screen screen, Type type, int key, int action) implements Event {
}
