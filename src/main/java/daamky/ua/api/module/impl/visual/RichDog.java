package daamky.ua.api.module.impl.visual;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.WorldRenderEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.ModeSetting;

public final class RichDog extends Module {
   private static final Identifier TEX_JACK_RUSSELL = Identifier.of("daamky", "textures/new/taksa.png");
   private static final Identifier TEX_DACHSHUND = Identifier.of("daamky", "textures/new/djekrussel.png");
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
   public void onTick(MinecraftClient client) {
      if (client.player != null) {
         this.brain.setEntity(client.player);
         this.brain.onUpdate();
      }
   }

   @SubscribeEvent
   private void onRender(WorldRenderEvent event) {
      if (mc.player != null && mc.world != null) {
         this.brain.setEntity(mc.player);
         MatrixStack pose = event.getStack();
         Immediate provider = mc.getBufferBuilders().getEntityVertexConsumers();
         Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();
         Vec3d render = this.brain.getPos().subtract(cam);
         Identifier texture = this.skin.is("Dachshund") ? TEX_DACHSHUND : TEX_JACK_RUSSELL;
         VertexConsumer consumer = provider.getBuffer(RenderLayers.entityCutoutNoCull(texture));
         pose.push();
         pose.translate(render.x, render.y, render.z);
         this.model.setupAnim((float)mc.player.age + event.getPartialTicks(), this.brain);
         this.model.render(pose, consumer, 15728880, OverlayTexture.DEFAULT_UV, this.brain);
         pose.pop();
         provider.draw(RenderLayers.entityCutoutNoCull(texture));
      }
   }
}
