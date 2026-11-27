package daamky.ua.api.module.impl.player;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.phys.Vec3;
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
   public Vec3 pos;
   public Vec3 prevPos;
   private RemotePlayer fakePlayerEntity;

   public FreeCam() {
      super("Free Cam", "Detached free camera.", ModuleCategory.PLAYER);
      instance = this;
   }

   public static FreeCam getInstance() {
      return instance;
   }

   @Override
   protected void onEnable() {
      Minecraft client = Minecraft.getInstance();
      Vec3 cameraPos = client.gameRenderer != null && client.gameRenderer.getMainCamera() != null
         ? client.gameRenderer.getMainCamera().position()
         : (client.player != null ? client.player.getEyePosition() : Vec3.ZERO);
      this.prevPos = this.pos = this.isFinite(cameraPos) ? cameraPos : Vec3.ZERO;
      this.spawnFakePlayerEntity(client);
   }

   @Override
   protected void onDisable() {
      this.resetRuntimeState();
      this.removeFakePlayerEntity(Minecraft.getInstance());
   }

   @SubscribeEvent
   private void onPacket(PacketEvent event) {
      if (event.getPacket() instanceof ServerboundMovePlayerPacket && this.freezeSetting.getValue()) {
         event.setCancelled(true);
      } else if (event.getPacket() instanceof ClientboundRespawnPacket || event.getPacket() instanceof ClientboundLoginPacket) {
         this.setEnabled(false);
      }
   }

   @SubscribeEvent
   private void onInput(InputEvent event) {
      if (this.pos != null) {
         float speed = this.speedSetting.getFloat();
         double[] motion = MoveUtil.calculateDirection((float)event.forward(), event.sideways(), (double)speed);
         this.prevPos = this.pos;
         this.pos = this.pos.add(motion[0], event.getInput().jump() ? (double)speed : (event.getInput().shift() ? (double)(-speed) : 0.0), motion[1]);
         event.inputNone();
      }
   }

   @SubscribeEvent
   private void onCameraPosition(CameraPositionEvent event) {
      Minecraft client = Minecraft.getInstance();
      if (!this.isFinite(this.prevPos) || !this.isFinite(this.pos)) {
         Vec3 fallback = this.isFinite(event.getPos()) ? event.getPos() : (client.player != null ? client.player.getEyePosition() : Vec3.ZERO);
         this.prevPos = fallback;
         this.pos = fallback;
      }

      if (this.isFinite(this.pos)) {
         float tickDelta = client.getDeltaTracker() != null ? Mth.clamp(client.getDeltaTracker().getGameTimeDeltaPartialTick(false), 0.0F, 1.0F) : 1.0F;
         event.setPos(this.prevPos.lerp(this.pos, (double)tickDelta));
      }

      if (client.options != null) {
         client.options.setCameraType(CameraType.FIRST_PERSON);
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

   private void spawnFakePlayerEntity(Minecraft client) {
      if (client.player != null && client.level != null) {
         this.removeFakePlayerEntity(client);
         GameProfile profile = new GameProfile(UUID.randomUUID(), this.resolveProfileName(client.player.getGameProfile()));
         RemotePlayer fake = new RemotePlayer(client.level, profile);
         fake.setId(-81374659);
         fake.copyPosition(client.player);
         fake.setYRot(client.player.getYRot());
         fake.setXRot(client.player.getXRot());
         fake.setYHeadRot(client.player.getYHeadRot());
         fake.setSprinting(client.player.isSprinting());
         fake.setShiftKeyDown(client.player.isShiftKeyDown());
         fake.setPose(client.player.getPose());
         fake.setHealth(client.player.getHealth());
         fake.setAbsorptionAmount(client.player.getAbsorptionAmount());

         for (int i = 0; i < client.player.getInventory().getContainerSize(); i++) {
            fake.getInventory().setItem(i, client.player.getInventory().getItem(i).copy());
         }

         fake.getInventory().setSelectedSlot(client.player.getInventory().getSelectedSlot());
         client.level.addEntity(fake);
         this.fakePlayerEntity = fake;
      }
   }

   private void removeFakePlayerEntity(Minecraft client) {
      if (client.level != null) {
         client.level.removeEntity(-81374659, RemovalReason.DISCARDED);
         if (this.fakePlayerEntity != null && this.fakePlayerEntity.getId() != -81374659) {
            client.level.removeEntity(this.fakePlayerEntity.getId(), RemovalReason.DISCARDED);
         }
      }

      if (this.fakePlayerEntity != null) {
         this.fakePlayerEntity.discard();
      }

      this.fakePlayerEntity = null;
   }

   private boolean isFinite(Vec3 vec) {
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
