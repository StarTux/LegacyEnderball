package com.winthier.minigames.enderball;

public class Kick {
    public static enum Strength {
        SHORT(0.8),
        LONG(1.4),
        ;
        public final double strength;
        Strength(double strength) { this.strength = strength; }
    }

    public static enum Height {
        LOW(0.5),
        HIGH(1.0),
        ;
        public final double height;
        Height(double height) { this.height = height; }
    }
}
