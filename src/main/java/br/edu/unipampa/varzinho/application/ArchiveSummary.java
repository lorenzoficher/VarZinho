package br.edu.unipampa.varzinho.application;

import java.time.Instant;
import java.util.Optional;

/** A day's readable count and latest capture, with any warning that qualifies them. */
public final class ArchiveSummary {
    private final int count;
    private final Optional<Instant> lastCapturedAt;
    private final Optional<String> warning;

    private ArchiveSummary(ArchiveListing newestFirst) {
        this.count = newestFirst.getHighlights().size();
        this.lastCapturedAt = newestFirst.getHighlights().stream().findFirst().map(HighlightView::getCapturedAt);
        this.warning = newestFirst.getWarning();
    }

    static ArchiveSummary from(ArchiveListing newestFirst) {
        return new ArchiveSummary(newestFirst);
    }

    /** {@return how many highlights could be read for the requested day} */
    public int getCount() {
        return count;
    }

    /** {@return the latest capture instant on the requested day, or empty when there are none} */
    public Optional<Instant> getLastCapturedAt() {
        return lastCapturedAt;
    }

    /** {@return the warning about unreadable highlights, or empty when every one was read} */
    public Optional<String> getWarning() {
        return warning;
    }
}
