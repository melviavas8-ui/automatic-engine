package daamky.ua.utils.timer;

import java.time.Instant;
import lombok.Generated;

public class TimerUtil {
   private long lastMS = System.currentTimeMillis();
   private long startTime;

   public void reset() {
      this.lastMS = Instant.now().toEpochMilli();
   }

   public TimerUtil() {
      this.resetCounter();
   }

   public static TimerUtil create() {
      return new TimerUtil();
   }

   public void resetCounter() {
      this.lastMS = System.currentTimeMillis();
   }

   public boolean isReached(long time) {
      return System.currentTimeMillis() - this.lastMS > time;
   }

   public void setLastMS(long newValue) {
      this.lastMS = System.currentTimeMillis() + newValue;
   }

   public void setTime(long time) {
      this.lastMS = time;
   }

   public long getTime() {
      return System.currentTimeMillis() - this.lastMS;
   }

   public boolean isRunning() {
      return System.currentTimeMillis() - this.lastMS <= 0L;
   }

   public boolean hasTimeElapsed(long time) {
      return System.currentTimeMillis() - this.lastMS > time;
   }

   public boolean finished(double delay) {
      return (double)System.currentTimeMillis() - delay >= (double)this.startTime;
   }

   public boolean hasTimeElapsed() {
      return this.lastMS < System.currentTimeMillis();
   }

   @Generated
   public long getLastMS() {
      return this.lastMS;
   }

   @Generated
   public long getStartTime() {
      return this.startTime;
   }
}
