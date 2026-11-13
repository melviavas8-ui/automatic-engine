package daamky.ua.api.events.impl;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil.Type;
import daamky.ua.api.events.Event;

public record KeyEvent(Screen screen, Type type, int key, int action) implements Event {
}
