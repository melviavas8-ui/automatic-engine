package daamky.ua.api.module.impl.combat.aura;

import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.utils.string.chat.ChatMessage;

public class AuraConfig {

    public static void applySpookyTime(AuraModule aura) {
        aura.mode.setValue("SpookyTime2");
        aura.attackRange.setValue(3.5);
        aura.lookRange.setValue(1.0);
        aura.moveFix.setValue("Focused");
        aura.onlyCriticals.setValue(true);
        ChatMessage.brandmessage("Applied SpookyTime Preset");
    }

    public static void applyFunTime(AuraModule aura) {
        aura.mode.setValue("FunTime");
        aura.attackRange.setValue(3.2);
        aura.lookRange.setValue(1.2);
        aura.moveFix.setValue("Free");
        ChatMessage.brandmessage("Applied FunTime Preset");
    }

    public static void applyReallyWorld(AuraModule aura) {
        aura.mode.setValue("ReallyWorld");
        aura.attackRange.setValue(3.4);
        aura.lookRange.setValue(0.8);
        aura.moveFix.setValue("Chase");
        ChatMessage.brandmessage("Applied ReallyWorld Preset");
    }

    public static void applyHypeMC(AuraModule aura) {
        aura.mode.setValue("SpookyTime2");
        aura.attackRange.setValue(3.8);
        aura.lookRange.setValue(1.0);
        aura.moveFix.setValue("Chase");
        ChatMessage.brandmessage("Applied HypeMC Preset");
    }

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