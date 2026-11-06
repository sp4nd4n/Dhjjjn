package com.spandan.taczsoldiers;

public enum BaseType {
    OUTPOST("Forward Outpost", 14, 16, 0),
    COMPOUND("Military Compound", 24, 28, 1),
    FACILITY("Classified Research Facility", 29, 33, 2);

    public final String displayName;
    public final int minRadius;
    public final int maxRadius;
    public final int sizeClass;

    BaseType(String displayName, int minRadius, int maxRadius, int sizeClass) {
        this.displayName = displayName;
        this.minRadius = minRadius;
        this.maxRadius = maxRadius;
        this.sizeClass = sizeClass;
    }
}
