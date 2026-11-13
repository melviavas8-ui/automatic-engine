package daamky.ua.utils.inventory.script;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import daamky.ua.api.module.impl.combat.aura.util.StopWatch;

public class Script {
   private final StopWatch time = new StopWatch();
   private final List<Script.ScriptStep> scriptSteps = Lists.newCopyOnWriteArrayList();
   private final List<Script.ScriptTickStep> scriptTickSteps = Lists.newCopyOnWriteArrayList();
   private int currentStepIndex;
   private int currentTickStepIndex;
   private boolean interrupt;
   private Script.LoopStrategy loopStrategy = new Script.FiniteLoopStrategy(1);

   public Script() {
      this.cleanup();
   }

   public Script addStep(int delay, ScriptAction action) {
      return this.addStep(delay, action, () -> true, 0);
   }

   public Script addStep(int delay, ScriptAction action, BooleanSupplier condition) {
      return this.addStep(delay, action, condition, 0);
   }

   public Script addStep(int delay, ScriptAction action, int priority) {
      return this.addStep(delay, action, () -> true, priority);
   }

   public Script addStep(int delay, ScriptAction action, BooleanSupplier condition, int priority) {
      this.scriptSteps.add(new Script.ScriptStep(delay, action, condition, priority));
      Collections.sort(this.scriptSteps);
      return this;
   }

   public Script addTickStep(int ticks, ScriptAction action) {
      return this.addTickStep(ticks, action, () -> true, 0);
   }

   public Script addTickStep(int ticks, ScriptAction action, BooleanSupplier condition) {
      return this.addTickStep(ticks, action, condition, 0);
   }

   public Script addTickStep(int ticks, ScriptAction action, int priority) {
      return this.addTickStep(ticks, action, () -> true, priority);
   }

   public Script addTickStep(int ticks, ScriptAction action, BooleanSupplier condition, int priority) {
      this.scriptTickSteps.add(new Script.ScriptTickStep(ticks, action, condition, priority));
      Collections.sort(this.scriptTickSteps);
      return this;
   }

   public void resetTime() {
      this.time.reset();
   }

   public void resetStepIndex() {
      this.currentStepIndex = 0;
      this.currentTickStepIndex = 0;
   }

   public Script cleanupIfFinished() {
      if (this.isFinished()) {
         this.cleanup();
      }

      return this;
   }

   public Script cleanup() {
      this.scriptSteps.clear();
      this.scriptTickSteps.clear();
      this.resetTime();
      this.resetStepIndex();
      return this;
   }

   public void update() {
      if ((!this.scriptSteps.isEmpty() || !this.scriptTickSteps.isEmpty()) && !this.interrupt) {
         this.scriptSteps.forEach(step -> {
            if (this.currentStepIndex < this.scriptSteps.size()) {
               Script.ScriptStep currentStep = this.scriptSteps.get(this.currentStepIndex);
               if (currentStep.condition().getAsBoolean() && this.time.finished((double)currentStep.delay())) {
                  currentStep.action().perform();
                  this.currentStepIndex++;
                  this.resetTime();
                  if (this.loopStrategy.shouldLoop(this.currentStepIndex, this.scriptSteps.size())) {
                     this.resetStepIndex();
                     this.loopStrategy.onLoop();
                  }
               }
            }
         });
         this.scriptTickSteps.forEach(step -> {
            if (this.currentTickStepIndex < this.scriptTickSteps.size()) {
               Script.ScriptTickStep currentTickStep = this.scriptTickSteps.get(this.currentTickStepIndex);
               if (currentTickStep.condition().getAsBoolean() && currentTickStep.ticks() <= 0) {
                  currentTickStep.action().perform();
                  this.currentTickStepIndex++;
                  this.resetTime();
                  if (this.loopStrategy.shouldLoop(this.currentTickStepIndex, this.scriptTickSteps.size())) {
                     this.resetStepIndex();
                     this.loopStrategy.onLoop();
                  }
               }

               currentTickStep.decrementTicks();
            }
         });
         this.currentStepIndex = Math.min(this.currentStepIndex, this.scriptSteps.size());
         this.currentTickStepIndex = Math.min(this.currentTickStepIndex, this.scriptTickSteps.size());
      }
   }

   public Script setLoopStrategy(Script.LoopStrategy loopStrategy) {
      this.loopStrategy = loopStrategy;
      return this;
   }

   public boolean isFinished() {
      return this.currentStepIndex >= this.scriptSteps.size()
         && this.currentTickStepIndex >= this.scriptTickSteps.size()
         && !this.interrupt
         && this.loopStrategy.isFinished();
   }

   public static class FiniteLoopStrategy implements Script.LoopStrategy {
      private final int loopCount;
      private int currentLoop;

      public FiniteLoopStrategy(int loopCount) {
         this.loopCount = loopCount - 1;
      }

      @Override
      public boolean shouldLoop(int currentStepIndex, int totalSteps) {
         return currentStepIndex >= totalSteps && this.currentLoop < this.loopCount;
      }

      @Override
      public void onLoop() {
         this.currentLoop++;
      }

      @Override
      public boolean isFinished() {
         return this.currentLoop >= this.loopCount;
      }
   }

   public static class InfiniteLoopStrategy implements Script.LoopStrategy {
      public InfiniteLoopStrategy() {
      }

      @Override
      public boolean shouldLoop(int currentStepIndex, int totalSteps) {
         return currentStepIndex >= totalSteps;
      }

      @Override
      public void onLoop() {
      }

      @Override
      public boolean isFinished() {
         return false;
      }
   }

   public interface LoopStrategy {
      boolean shouldLoop(int var1, int var2);

      void onLoop();

      boolean isFinished();
   }

   public static final class ScriptStep implements Comparable<Script.ScriptStep> {
      private int delay;
      private ScriptAction action;
      private BooleanSupplier condition;
      private int priority;

      public ScriptStep(int delay, ScriptAction action, BooleanSupplier condition, int priority) {
         this.delay = delay;
         this.action = action;
         this.condition = condition;
         this.priority = priority;
      }

      public int delay() {
         return this.delay;
      }

      public ScriptAction action() {
         return this.action;
      }

      public BooleanSupplier condition() {
         return this.condition;
      }

      public int priority() {
         return this.priority;
      }

      public int compareTo(Script.ScriptStep otherStep) {
         return Integer.compare(otherStep.priority(), this.priority());
      }
   }

   public static final class ScriptTickStep implements Comparable<Script.ScriptTickStep> {
      private int ticks;
      private ScriptAction action;
      private BooleanSupplier condition;
      private int priority;

      public ScriptTickStep(int ticks, ScriptAction action, BooleanSupplier condition, int priority) {
         this.ticks = ticks;
         this.action = action;
         this.condition = condition;
         this.priority = priority;
      }

      public int ticks() {
         return this.ticks;
      }

      public ScriptAction action() {
         return this.action;
      }

      public BooleanSupplier condition() {
         return this.condition;
      }

      public int priority() {
         return this.priority;
      }

      public int compareTo(Script.ScriptTickStep otherStep) {
         return Integer.compare(otherStep.priority(), this.priority());
      }

      public void decrementTicks() {
         this.ticks--;
      }
   }
}
