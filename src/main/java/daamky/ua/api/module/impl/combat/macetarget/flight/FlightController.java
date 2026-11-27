package daamky.ua.api.module.impl.combat.macetarget.flight;

import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.MathAngle;
import daamky.ua.api.module.impl.combat.macetarget.prediction.TargetPredictor;
import daamky.ua.api.module.impl.combat.macetarget.state.MaceState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class FlightController {
   private static final Minecraft MC = Minecraft.getInstance();
   private final TargetPredictor predictor;
   private boolean predictionEnabled;
   private float height = 30.0F;

   public FlightController(TargetPredictor predictor) {
      this.predictor = predictor;
   }

   public void setPredictionEnabled(boolean predictionEnabled) {
      this.predictionEnabled = predictionEnabled;
   }

   public void setHeight(float height) {
      this.height = height;
   }

   public Vec3 getTargetPosition(LivingEntity target, MaceState.Stage stage) {
      if (target == null) {
         return Vec3.ZERO;
      } else {
         return this.predictionEnabled && this.predictor.isMoving() ? this.predictor.getPredictedPosition(target, stage) : target.getEyePosition();
      }
   }

   public Angle calculateAngle(LivingEntity target, MaceState.Stage stage) {
      if (target != null && MC.player != null) {
         Vec3 targetPos = this.getTargetPosition(target, stage);
         if (stage == MaceState.Stage.FLYING_UP) {
            return MathAngle.fromVec3d(targetPos.add(0.0, (double)this.height, 0.0).subtract(MC.player.getEyePosition()));
         } else {
            return stage != MaceState.Stage.TARGETTING && stage != MaceState.Stage.ATTACKING
               ? MathAngle.cameraAngle()
               : MathAngle.fromVec3d(targetPos.subtract(MC.player.getEyePosition()));
         }
      } else {
         return MathAngle.cameraAngle();
      }
   }
}
