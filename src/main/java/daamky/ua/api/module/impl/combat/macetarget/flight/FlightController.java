package daamky.ua.api.module.impl.combat.macetarget.flight;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.MathAngle;
import daamky.ua.api.module.impl.combat.macetarget.prediction.TargetPredictor;
import daamky.ua.api.module.impl.combat.macetarget.state.MaceState;

public class FlightController {
   private static final MinecraftClient MC = MinecraftClient.getInstance();
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

   public Vec3d getTargetPosition(LivingEntity target, MaceState.Stage stage) {
      if (target == null) {
         return Vec3d.ZERO;
      } else {
         return this.predictionEnabled && this.predictor.isMoving() ? this.predictor.getPredictedPosition(target, stage) : target.getEyePos();
      }
   }

   public Angle calculateAngle(LivingEntity target, MaceState.Stage stage) {
      if (target != null && MC.player != null) {
         Vec3d targetPos = this.getTargetPosition(target, stage);
         if (stage == MaceState.Stage.FLYING_UP) {
            return MathAngle.fromVec3d(targetPos.add(0.0, (double)this.height, 0.0).subtract(MC.player.getEyePos()));
         } else {
            return stage != MaceState.Stage.TARGETTING && stage != MaceState.Stage.ATTACKING
               ? MathAngle.cameraAngle()
               : MathAngle.fromVec3d(targetPos.subtract(MC.player.getEyePos()));
         }
      } else {
         return MathAngle.cameraAngle();
      }
   }
}
