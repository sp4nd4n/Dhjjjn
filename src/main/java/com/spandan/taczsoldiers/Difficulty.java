package com.spandan.taczsoldiers;

import net.minecraft.ChatFormatting;

public enum Difficulty {
    EASY("Easy", ChatFormatting.GREEN),
    NORMAL("Normal", ChatFormatting.YELLOW),
    HARD("Hard", ChatFormatting.RED),
    EXTREME("Extreme", ChatFormatting.DARK_RED);

    public final String displayName;
    public final ChatFormatting color;

    Difficulty(String displayName, ChatFormatting color) {
        this.displayName = displayName;
        this.color = color;
    }

    /** Harder missions guard more valuable objectives. */
    public static Difficulty forValue(int value) {
        return switch (value) {
            case 1 -> EASY;
            case 2 -> NORMAL;
            case 5 -> EXTREME;
            default -> HARD;
        };
    }
}
