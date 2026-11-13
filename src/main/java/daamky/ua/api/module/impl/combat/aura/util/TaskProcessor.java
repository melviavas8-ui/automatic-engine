package daamky.ua.api.module.impl.combat.aura.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public final class TaskProcessor<T> {
   private final List<TaskProcessor.Task<T>> activeTasks = new ArrayList<>();
   private int tickCounter;

   public TaskProcessor() {
   }

   public void addTask(TaskProcessor.Task<T> task) {
      if (task != null) {
         this.activeTasks.add(task);
         this.activeTasks.sort(Comparator.comparingInt((TaskProcessor.Task<T> t) -> t.priority()).reversed());
      }
   }

   public T fetchActiveTaskValue() {
      return this.activeTasks.isEmpty() ? null : this.activeTasks.getFirst().value();
   }

   public int tickCounter() {
      return this.tickCounter;
   }

   public void tick(int ticks) {
      this.tickCounter += ticks;
      Iterator<TaskProcessor.Task<T>> iterator = this.activeTasks.iterator();

      while (iterator.hasNext()) {
         TaskProcessor.Task<T> task = iterator.next();
         task.age += ticks;
         if (task.age >= task.duration()) {
            iterator.remove();
         }
      }

      if (this.activeTasks.isEmpty()) {
         this.tickCounter = 0;
      }
   }

   public void clear() {
      this.activeTasks.clear();
      this.tickCounter = 0;
   }

   public static final class Task<T> {
      private final int duration;
      private final int priority;
      private final Object provider;
      private final T value;
      private int age;

      public Task(int duration, int priority, Object provider, T value) {
         this.duration = Math.max(1, duration);
         this.priority = priority;
         this.provider = provider;
         this.value = value;
      }

      public int duration() {
         return this.duration;
      }

      public int priority() {
         return this.priority;
      }

      public Object provider() {
         return this.provider;
      }

      public T value() {
         return this.value;
      }
   }
}
