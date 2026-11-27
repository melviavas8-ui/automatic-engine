package daamky.ua.api.module.impl.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.WorldRenderEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public final class RichDog extends Module {
   private static final Identifier TEX_JACK_RUSSELL = Identifier.fromNamespaceAndPath("daamky", "textures/new/taksa.png");
   private static final Identifier TEX_DACHSHUND = Identifier.fromNamespaceAndPath("daamky", "textures/new/djekrussel.png");
   private final PetModel model = new PetModel();
   private final PetBrain brain = new PetBrain();
   private final ModeSetting skin = this.register(new ModeSetting("Skin", "Dog breed.", "Jack Russell", "Jack Russell", "Dachshund"));

   public RichDog() {
      super("Richi Dog", "A loyal follower dog.", ModuleCategory.VISUAL);
   }

   @Override
   protected void onDisable() {
      this.brain.setEntity(null);
   }

   @Override
   public void onTick(Minecraft client) {
      if (client.player != null) {
         this.brain.setEntity(client.player);
         this.brain.onUpdate();
      }
   }

   @SubscribeEvent
   private void onRender(WorldRenderEvent event) {
      if (mc.player != null && mc.level != null) {
         this.brain.setEntity(mc.player);
         PoseStack pose = event.getStack();
         BufferSource provider = mc.renderBuffers().bufferSource();
         Vec3 cam = mc.gameRenderer.getMainCamera().position();
         Vec3 render = this.brain.getPos().subtract(cam);
         Identifier texture = this.skin.is("Dachshund") ? TEX_DACHSHUND : TEX_JACK_RUSSELL;
         VertexConsumer consumer = provider.getBuffer(RenderTypes.entityCutoutNoCull(texture));
         pose.pushPose();
         pose.translate(render.x, render.y, render.z);
         this.model.setupAnim((float)mc.player.tickCount + event.getPartialTicks(), this.brain);
         this.model.render(pose, consumer, 15728880, OverlayTexture.NO_OVERLAY, this.brain);
         pose.popPose();
         provider.endBatch(RenderTypes.entityCutoutNoCull(texture));
      }
   }
}
