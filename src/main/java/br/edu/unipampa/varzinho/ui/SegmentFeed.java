package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.domain.capture.Frame;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.stream.SegmentIndex;
import br.edu.unipampa.varzinho.stream.SegmentLog;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;

/**
 * The real-time twin of {@link LiveFeed}: a second of footage reaches the court when
 * the phone recorder finishes its segment, not when a timer says so.
 *
 * <p>It feeds the phone's court alone: a segment is footage from the phone, and any
 * other court recording it would capture what that court's own camera never saw.
 *
 * <p>It polls faster than once a second so a finished segment is never late by more
 * than a fraction of one. Like the live feed, it holds no capture rule.
 */
final class SegmentFeed implements Feed {

    private static final int POLL_MILLIS = 250;

    private final Court phoneCourt;
    private final SegmentLog log;
    private final SegmentIndex index;
    private int sequence;

    SegmentFeed(Court phoneCourt, SegmentLog log, SegmentIndex index) {
        if (phoneCourt == null) throw new IllegalArgumentException("a segment feed needs the phone's court");
        if (log == null) throw new IllegalArgumentException("a segment feed needs the recorder's segment list");
        if (index == null) throw new IllegalArgumentException("a segment feed needs an index to fill");
        this.phoneCourt = phoneCourt;
        this.log = log;
        this.index = index;
    }

    @Override
    public int periodMillis() {
        return POLL_MILLIS;
    }

    /**
     * Records one second into the phone's court for each segment finished since the
     * last call, remembering which file holds it.
     */
    @Override
    public void tick(Instant now) throws IOException {
        for (Path segment : log.readNew()) {
            index.register(sequence, segment);
            phoneCourt.record(new Frame(now, sequence++));
        }
    }
}
