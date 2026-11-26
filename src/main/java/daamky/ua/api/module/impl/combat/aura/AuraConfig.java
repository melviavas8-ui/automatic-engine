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