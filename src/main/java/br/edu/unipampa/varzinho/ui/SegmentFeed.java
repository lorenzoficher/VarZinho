package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.domain.capture.Camera;
import br.edu.unipampa.varzinho.domain.capture.Frame;
import br.edu.unipampa.varzinho.stream.SegmentIndex;
import br.edu.unipampa.varzinho.stream.SegmentLog;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

/**
 * The real-time twin of {@link LiveFeed}: a second of footage reaches the phone when
 * the phone recorder finishes its segment, not when a timer says so.
 *
 * <p>It feeds the phone alone: a segment is footage from the phone, and any other
 * camera recording it would capture what that camera never saw.
 *
 * <p>A phone that stops sending segments is switched off. Its buffer would otherwise
 * keep the seconds from before the silence, and every capture would hand them out as
 * the last thirty seconds.
 *
 * <p>It polls faster than once a second so a finished segment is never late by more
 * than a fraction of one. Like the live feed, it holds no capture rule.
 */
final class SegmentFeed implements Feed {

    static final Duration SILENCE_LIMIT = Duration.ofSeconds(5);

    private static final int POLL_MILLIS = 250;

    private final Camera phone;
    private final SegmentLog log;
    private final SegmentIndex index;
    private final Runnable onSilence;
    private int sequence;
    private Instant lastHeard;
    private boolean silenced;

    /**
     * @param onSilence told once, after the phone has been switched off for sending
     *     nothing for {@link #SILENCE_LIMIT}
     */
    SegmentFeed(Camera phone, SegmentLog log, SegmentIndex index, Runnable onSilence) {
        if (phone == null) throw new IllegalArgumentException("a segment feed needs the phone it feeds");
        if (log == null) throw new IllegalArgumentException("a segment feed needs the recorder's segment list");
        if (index == null) throw new IllegalArgumentException("a segment feed needs an index to fill");
        if (onSilence == null) throw new IllegalArgumentException("a segment feed needs someone to tell of a silence");
        this.phone = phone;
        this.log = log;
        this.index = index;
        this.onSilence = onSilence;
    }

    @Override
    public int periodMillis() {
        return POLL_MILLIS;
    }

    /**
     * Records one second into the phone for each segment finished since the last call,
     * remembering which file holds it. When none has arrived for {@link #SILENCE_LIMIT}
     * — counted from the first call, if none ever has — switches the phone off.
     */
    @Override
    public void tick(Instant now) throws IOException {
        if (lastHeard == null) {
            lastHeard = now;
        }
        for (Path segment : log.readNew()) {
            index.register(sequence, segment);
            phone.record(new Frame(now, sequence++));
            lastHeard = now;
        }
        if (!silenced && Duration.between(lastHeard, now).compareTo(SILENCE_LIMIT) >= 0) {
            silenced = true;
            if (phone.isRecording()) {
                phone.stopRecording();
            }
            onSilence.run();
        }
    }
}
