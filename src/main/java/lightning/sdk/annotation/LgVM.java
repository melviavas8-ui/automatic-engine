package lightning.sdk.annotation;

public @interface LgVM {
    enum Level {
        FULL,
        MEDIUM,
        HARD,
        LOW,
        VIRTUALIZATION,
        NONE,
        PARTIAL
    }

    Level value() default Level.MEDIUM;
}
