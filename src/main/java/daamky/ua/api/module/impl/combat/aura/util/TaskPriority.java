package daamky.ua.api.module.impl.combat.aura.util;

public enum TaskPriority {
   HIGH_IMPORTANCE_1(100),
   HIGH_IMPORTANCE_2(90),
   NORMAL(0);

   private final int priority;

   private TaskPriority(int priority) {
      this.priority = priority;
   }

   public int getPriority() {
      return this.priority;
   }
}
