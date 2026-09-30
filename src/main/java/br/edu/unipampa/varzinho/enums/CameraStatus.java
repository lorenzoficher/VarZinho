package br.edu.unipampa.varzinho.enums;

/**
 * Operational state of a camera, and the rule that decides whether it records.
 *
 * <p>The rule lives here rather than at the call sites: a court asking whether it
 * can capture, and a camera deciding whether to accept a frame, ask the same
 * question of the same object.
 */
public enum CameraStatus {

    ACTIVE,
    INACTIVE,
    MAINTENANCE;

    /**
     * Whether a camera in this state takes footage.
     *
     * @return {@code true} only for {@link #ACTIVE}
     */
    public boolean canRecord() {
        return this == ACTIVE;
    }
}
