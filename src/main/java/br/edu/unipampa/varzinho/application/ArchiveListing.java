package br.edu.unipampa.varzinho.application;

import java.util.List;
import java.util.Optional;

/**
 * The readable archive rows and, when recovery was needed, the warning to show with them.
 */
public final class ArchiveListing {

    private final List<HighlightView> highlights;
    private final String warning;

    private ArchiveListing(List<HighlightView> highlights, String warning) {
        this.highlights = List.copyOf(highlights);
        this.warning = warning;
    }

    static ArchiveListing complete(List<HighlightView> highlights) {
        return new ArchiveListing(highlights, null);
    }

    static ArchiveListing recovered(List<HighlightView> highlights, String warning) {
        return new ArchiveListing(highlights, warning);
    }

    public List<HighlightView> getHighlights() {
        return highlights;
    }

    public Optional<String> getWarning() {
        return Optional.ofNullable(warning);
    }
}
