package com.spandan.taczsoldiers;

/** The secret objects hidden in bases. Higher value = better guarded base. */
public enum ConfidentialType {
    DOCUMENTS("classified_documents", "Classified War Plans", 1),
    DRIVE("intel_drive", "Encrypted Intel Drive", 2),
    CASE("prototype_case", "Prototype Weapon Case", 3),
    CODES("launch_codes", "Nuclear Launch Codes", 4),
    CORE("reactor_core", "Experimental Reactor Core", 5);

    public final String id;
    public final String displayName;
    public final int value;

    ConfidentialType(String id, String displayName, int value) {
        this.id = id;
        this.displayName = displayName;
        this.value = value;
    }

    public static ConfidentialType byValue(int value) {
        for (ConfidentialType t : values()) {
            if (t.value == value) {
                return t;
            }
        }
        return DOCUMENTS;
    }
}
