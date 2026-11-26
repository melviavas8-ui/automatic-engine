package daamky.ua.api.module.impl.combat.aura;

import java.util.Set;
import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.utils.string.chat.ChatMessage;

public final class AuraConfig {
    private AuraConfig() {}

    public static void applySpookyTime(AuraModule a) {
        a.mode.setValue("SpookyTime2");
        a.attackRange.setValue(3.1);
        a.lookRange.setValue(0.9);
        a.keepTargetRange.setValue(4.4);
        a.targetType.setValue(Set.of("Players", "Mobs", "Animals", "Naked Players"));
        a.targetPriority.setValue("Distance");
        a.options.setValue(Set.of("Pause While Using", "Release Shield"));
        a.moveFix.setValue("Target");
        a.onlyCriticals.setValue(true);
        a.smartCriticals.setValue(true);
        a.criticalBalance.setValue(false);
        a.hitMoment.setValue("Legit");
        a.randomFallMin.setValue(0.0);
        a.randomFallMax.setValue(0.25);
        a.staticFall.setValue(0.0);
        a.maxDamage.setValue(false);
        a.legitfov.setValue(true);
        ChatMessage.brandmessage("Applied SpookyTime Preset");
    }

    public static void applyFunTime(AuraModule a) {
        a.mode.setValue("Legit Snap");
        a.attackRange.setValue(3.1);
        a.lookRange.setValue(0.9);
        a.keepTargetRange.setValue(4.4);
        a.targetType.setValue(Set.of("Players", "Mobs", "Naked Players"));
        a.targetPriority.setValue("Distance");
        a.options.setValue(Set.of("Pause While Using", "Release Shield"));
        a.moveFix.setValue("Free");
        a.onlyCriticals.setValue(true);
        a.smartCriticals.setValue(true);
        a.criticalBalance.setValue(false);
        a.hitMoment.setValue("Legit");
        a.randomFallMin.setValue(0.0);
        a.randomFallMax.setValue(0.25);
        a.staticFall.setValue(0.0);
        a.maxDamage.setValue(false);
        a.legitfov.setValue(true);
        ChatMessage.brandmessage("Applied FunTime Preset");
    }

    public static void applyReallyWorld(AuraModule a) {
        a.mode.setValue("ReallyWorld");
        a.attackRange.setValue(3.1);
        a.lookRange.setValue(1.2);
        a.keepTargetRange.setValue(4.8);
        a.targetType.setValue(Set.of("Players", "Mobs", "Animals", "Naked Players"));
        a.targetPriority.setValue("Distance");
        a.options.setValue(Set.of("Pause While Using", "Release Shield"));
        a.moveFix.setValue("Chase");
        a.onlyCriticals.setValue(true);
        a.smartCriticals.setValue(false);
        a.criticalBalance.setValue(false);
        a.hitMoment.setValue("Static");
        a.randomFallMin.setValue(0.0);
        a.randomFallMax.setValue(0.25);
        a.staticFall.setValue(0.0);
        a.maxDamage.setValue(false);
        a.legitfov.setValue(true);
        ChatMessage.brandmessage("Applied ReallyWorld Preset");
    }

    public static void applyHypeMC(AuraModule a) {
        a.mode.setValue("ReallyWorld");
        a.attackRange.setValue(3.1);
        a.lookRange.setValue(0.9);
        a.keepTargetRange.setValue(4.4);
        a.targetType.setValue(Set.of("Players", "Mobs", "Animals", "Naked Players"));
        a.targetPriority.setValue("Distance");
        a.options.setValue(Set.of("Pause While Using", "Release Shield"));
        a.moveFix.setValue("Free");
        a.onlyCriticals.setValue(true);
        a.smartCriticals.setValue(true);
        a.criticalBalance.setValue(false);
        a.hitMoment.setValue("Legit");
        a.randomFallMin.setValue(0.0);
        a.randomFallMax.setValue(0.25);
        a.staticFall.setValue(0.0);
        a.maxDamage.setValue(false);
        a.legitfov.setValue(true);
        ChatMessage.brandmessage("Applied HypeMC Preset");
    }

    // --- Новые пресеты для Shard ---

    public static void applySpaceTimesTest(AuraModule aura) {
        aura.mode.setValue("Shard");
        aura.attackRange.setValue(3.1);
        aura.lookRange.setValue(0.9);
        aura.keepTargetRange.setValue(4.4);
        
        // Targets
        aura.targetType.setSelected("Players", true);
        aura.targetType.setSelected("Mobs", true);
        aura.targetType.setSelected("Naked Players", true);
        
        aura.targetPriority.setValue("Health");
        
        // Options
        aura.options.setSelected("Pause While Using", true);
        aura.options.setSelected("Release Shield", true);
        
        aura.moveFix.setValue("Free");
        aura.onlyCriticals.setValue(true);
        aura.smartCriticals.setValue(true);
        aura.criticalBalance.setValue(false);
        aura.hitMoment.setValue("Legit");
        
        ChatMessage.brandmessage("Applied SpaceTimes Test Preset");
    }

    public static void applySkyTimeTest(AuraModule aura) {
        aura.mode.setValue("Shard");
        aura.attackRange.setValue(3.1);
        aura.lookRange.setValue(0.9);
        aura.keepTargetRange.setValue(4.4);
        
        // Targets
        aura.targetType.setSelected("Players", true);
        
        aura.targetPriority.setValue("Distance");
        
        // Options
        aura.options.setSelected("Pause While Using", true);
        aura.options.setSelected("Release Shield", true);
        
        aura.moveFix.setValue("Free");
        aura.onlyCriticals.setValue(true);
        aura.smartCriticals.setValue(true);
        aura.criticalBalance.setValue(false);
        aura.hitMoment.setValue("Legit");
        
        ChatMessage.brandmessage("Applied SkyTime Test Preset");
    }
}