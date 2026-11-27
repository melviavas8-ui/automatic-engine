package daamky.ua.api.events.impl;

import daamky.ua.api.events.Event;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public record BlockBreakingEvent(BlockPos blockPos, Direction direction) implements Event {
}
