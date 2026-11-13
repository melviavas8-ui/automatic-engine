package daamky.ua.api.events.impl;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import daamky.ua.api.events.Event;

public record BlockBreakingEvent(BlockPos blockPos, Direction direction) implements Event {
}
