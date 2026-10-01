package br.edu.unipampa.varzinho.ui;

import java.io.IOException;
import java.time.Instant;

/**
 * Where the window's seconds of footage come from: a simulated clock, or the segments
 * the phone recorder finishes. The window drives either one from the same timer.
 */
interface Feed {

    /** How often the window should call {@link #tick}. */
    int periodMillis();

    /**
     * Hands the courts whatever seconds of footage became available since the last call.
     *
     * @throws IOException if the footage could not be read this time; the next call tries again
     */
    void tick(Instant now) throws IOException;
}
