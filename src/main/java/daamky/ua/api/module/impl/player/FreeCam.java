package daamky.ua.api.module.impl.player;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.CameraPositionEvent;
import daamky.ua.api.events.impl.ChunkOcclusionEvent;
import daamky.ua.api.events.impl.InputEvent;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.move.MoveUtil;

public final class FreeCam extends Module {
   private static final int FAKE_PLAYER_ENTITY_ID = -81374659;
   private static FreeCam instance;
   private final NumberSetting speedSetting = this.register(new NumberSetting("Speed", "Free camera speed.", 2.0, 0.5, 5.0, 0.1));
   private final BooleanSetting freezeSetting = this.register(new BooleanSetting("Freeze", "Freeze player on place.", false));
   public Vec3d pos;
   public Vec3d prevPos;
   private OtherClientPlayerEntity fakePlayerEntity;

   public FreeCam() {
      super("Free Cam", "Detached free camera.", ModuleCategory.PLAYER);
      instance = this;
   }

   public static FreeCam getInstance() {
      return instance;
   }

   @Override
   protected void onEnable() {
      MinecraftClient client = MinecraftClient.getInstance();
      Vec3d cameraPos = client.gameRenderer != null && client.gameRenderer.getCamera() != null
         ? client.gameRenderer.getCamera().getCameraPos()
         : (client.player != null ? client.player.getEyePos() : Vec3d.ZERO);
      this.prevPos = this.pos = this.isFinite(cameraPos) ? cameraPos : Vec3d.ZERO;
      this.spawnFakePlayerEntity(client);
   }

   @Override
   protected void onDisable() {
      this.resetRuntimeState();
      this.removeFakePlayerEntity(MinecraftClient.getInstance());
   }

   @SubscribeEvent
   private void onPacket(PacketEvent event) {
      if (event.getPacket() instanceof PlayerMoveC2SPacket && this.freezeSetting.getValue()) {
         event.setCancelled(true);
      } else if (event.getPacket() instanceof PlayerRespawnS2CPacket || event.getPacket() instanceof GameJoinS2CPacket) {
         this.setEnabled(false);
      }
   }

   @SubscribeEvent
   private void onInput(InputEvent event) {
      if (this.pos != null) {
         float speed = this.speedSetting.getFloat();
         double[] motion = MoveUtil.calculateDirection((float)event.forward(), event.sideways(), (double)speed);
         this.prevPos = this.pos;
         this.pos = this.pos.add(motion[0], event.getInput().jump() ? (double)speed : (event.getInput().sneak() ? (double)(-speed) : 0.0), motion[1]);
         event.inputNone();
      }
   }

   @SubscribeEvent
   private void onCameraPosition(CameraPositionEvent event) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (!this.isFinite(this.prevPos) || !this.isFinite(this.pos)) {
         Vec3d fallback = this.isFinite(event.getPos()) ? event.getPos() : (client.player != null ? client.player.getEyePos() : Vec3d.ZERO);
         this.prevPos = fallback;
         this.pos = fallback;
      }

      if (this.isFinite(this.pos)) {
         float tickDelta = client.getRenderTickCounter() != null ? MathHelper.clamp(client.getRenderTickCounter().getTickProgress(false), 0.0F, 1.0F) : 1.0F;
         event.setPos(this.prevPos.lerp(this.pos, (double)tickDelta));
      }

      if (client.options != null) {
         client.options.setPerspective(Perspective.FIRST_PERSON);
      }
   }

   @SubscribeEvent
   private void onChunkOcclusion(ChunkOcclusionEvent event) {
      event.setCancelled(true);
   }

   public boolean hasDetachedCameraState() {
      return this.isFinite(this.pos) || this.isFinite(this.prevPos) || this.fakePlayerEntity != null;
   }

   public void resetRuntimeState() {
      this.pos = null;
      this.prevPos = null;
   }

   private void spawnFakePlayerEntity(MinecraftClient client) {
      if (client.player != null && client.world != null) {
         this.removeFakePlayerEntity(client);
         GameProfile profile = new GameProfile(UUID.randomUUID(), this.resolveProfileName(client.player.getGameProfile()));
         OtherClientPlayerEntity fake = new OtherClientPlayerEntity(client.world, profile);
         fake.setId(-81374659);
         fake.copyPositionAndRotation(client.player);
         fake.setYaw(client.player.getYaw());
         fake.setPitch(client.player.getPitch());
         fake.setHeadYaw(client.player.getHeadYaw());
         fake.setSprinting(client.player.isSprinting());
         fake.setSneaking(client.player.isSneaking());
         fake.setPose(client.player.getPose());
         fake.setHealth(client.player.getHealth());
         fake.setAbsorptionAmount(client.player.getAbsorptionAmount());

         for (int i = 0; i < client.player.getInventory().size(); i++) {
            fake.getInventory().setStack(i, client.player.getInventory().getStack(i).copy());
         }

         fake.getInventory().setSelectedSlot(client.player.getInventory().getSelectedSlot());
         client.world.addEntity(fake);
         this.fakePlayerEntity = fake;
      }
   }

   private void removeFakePlayerEntity(MinecraftClient client) {
      if (client.world != null) {
         client.world.removeEntity(-81374659, RemovalReason.DISCARDED);
         if (this.fakePlayerEntity != null && this.fakePlayerEntity.getId() != -81374659) {
            client.world.removeEntity(this.fakePlayerEntity.getId(), RemovalReason.DISCARDED);
         }
      }

      if (this.fakePlayerEntity != null) {
         this.fakePlayerEntity.discard();
      }

      this.fakePlayerEntity = null;
   }

   private boolean isFinite(Vec3d vec) {
      return vec != null && Double.isFinite(vec.x) && Double.isFinite(vec.y) && Double.isFinite(vec.z);
   }

   private String resolveProfileName(GameProfile profile) {
      Object value = this.invokeProfileMethod(profile, "name");
      if (!(value instanceof String)) {
         value = this.invokeProfileMethod(profile, "getName");
      }

      return value instanceof String name ? name : "FreeCam";
   }

   private Object invokeProfileMethod(GameProfile profile, String methodName) {
      try {
         return GameProfile.class.getMethod(methodName).invoke(profile);
      } catch (ReflectiveOperationException var4) {
         return null;
      }
   }
}
