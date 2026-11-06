package com.spandan.taczsoldiers;

public enum MissionType {
    ASSAULT("Assault", "Full-scale assault. Bring firepower.",
            new String[][]{{"rifle", "smg"}, {"rifle", "smg"}, {"smg", "rifle"}}, 4),
    OVERWATCH("Overwatch", "Long-range operation. Pick them off.",
            new String[][]{{"sniper", "rifle"}, {"pistol", "smg"}, {"smg", "rifle"}}, 2),
    DEMOLITION("Breach & Destroy", "Heavy ordnance for a hard entry.",
            new String[][]{{"rpg", "shotgun", "rifle"}, {"shotgun", "smg"}, {"smg", "rifle"}}, 6),
    STEALTH("Silent Strike", "Quiet and fast. Dress for the part.",
            new String[][]{{"smg", "rifle"}, {"pistol", "smg"}, {"sniper", "rifle"}}, 3);

    public final String displayName;
    public final String blurb;
    /** One entry per gun in the crate: gun classes in priority order. */
    public final String[][] guns;
    public final int grenades;

    MissionType(String displayName, String blurb, String[][] guns, int grenades) {
        this.displayName = displayName;
        this.blurb = blurb;
        this.guns = guns;
        this.grenades = grenades;
    }
}
