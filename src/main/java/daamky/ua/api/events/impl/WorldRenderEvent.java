package daamky.ua.api.events.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import daamky.ua.api.events.Event;

public record WorldRenderEvent(PoseStack poseStack, float tickDelta) implements Event {
   public PoseStack getStack() {
      return this.poseStack;
   }

   public float getTickDelta() {
      return this.tickDelta;
   }

   public float getPartialTicks() {
      return this.tickDelta;
   }
}
