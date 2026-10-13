package com.coala.appliedpowah.orb;

/**
 * Product ejection modes for every orb tier (issues.md 其他-10).
 *
 * <pre>
 * OFF     — products stay in the page output slot (CPU orders hang by design;
 *           switching back resumes). Same semantics as the 0.4.9
 *           "autoExport OFF = nothing leaves the machine" rule.
 * SIDES   — push into the configured {@link #allowedOutputs} faces only
 *           (AAE reaction-chamber style directional output).
 * NETWORK — old autoExport ON: OUTPUT into ME storage and CPU returnInv
 *           back into the network.
 * </pre>
 */
public enum EjectMode {
    OFF("applied_powah.gui.eject.off"),
    SIDES("applied_powah.gui.eject.sides"),
    NETWORK("applied_powah.gui.eject.network");

    private final String langKey;

    EjectMode(String langKey) {
        this.langKey = langKey;
    }

    public String langKey() {
        return langKey;
    }

    /** Toolbar button cycles through all three states in declaration order. */
    public EjectMode next() {
        EjectMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    /** True when products are allowed to leave the page output slot. */
    public boolean ejects() {
        return this != OFF;
    }
}
