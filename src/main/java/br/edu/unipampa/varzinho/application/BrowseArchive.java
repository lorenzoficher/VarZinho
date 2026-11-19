package br.edu.unipampa.varzinho.application;

import br.edu.unipampa.varzinho.application.port.CorruptedRecordException;
import br.edu.unipampa.varzinho.application.port.HighlightRepository;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.domain.highlight.Highlight;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Lists saved highlights without exposing persistence or domain objects to the caller. */
public final class BrowseArchive {

    private final HighlightRepository repository;
    private final Clock clock;
    private final ZoneId zone;

    public BrowseArchive(HighlightRepository repository) {
        this(repository, Clock.systemDefaultZone(), ZoneId.systemDefault());
    }

    /** The zone is the gym's local zone; it can differ from the clock's own zone. */
    public BrowseArchive(HighlightRepository repository, Clock clock, ZoneId zone) {
        if (repository == null) {
            throw new IllegalArgumentException("browsing the archive needs a highlight repository");
        }
        if (clock == null) throw new IllegalArgumentException("browsing the archive needs a clock");
        if (zone == null) throw new IllegalArgumentException("browsing the archive needs the gym's time zone");
        this.repository = repository;
        this.clock = clock;
        this.zone = zone;
    }

    public ArchiveListing all() throws RepositoryException {
        return browse(repository::findAll);
    }

    public ArchiveListing byCourt(int courtNumber) throws RepositoryException {
        return browse(() -> repository.findByCourt(courtNumber));
    }

    /** Lists one local calendar day, newest first, retaining any recovery warning. */
    public ArchiveListing byDay(LocalDate day) throws RepositoryException {
        return browse(repository::findAll, capturedOn(day));
    }

    /** Lists that local day on one court, newest first. */
    public ArchiveListing byDay(LocalDate day, int courtNumber) throws RepositoryException {
        return browse(() -> repository.findByCourt(courtNumber), capturedOn(day));
    }

    private Predicate<Highlight> capturedOn(LocalDate day) {
        if (day == null) throw new IllegalArgumentException("browsing a day needs a date");
        return highlight -> highlight.getCapturedAt().atZone(zone).toLocalDate().equals(day);
    }

    private static ArchiveListing browse(ArchiveLookup lookup) throws RepositoryException {
        return browse(lookup, highlight -> true);
    }

    private static ArchiveListing browse(ArchiveLookup lookup, Predicate<Highlight> filter) throws RepositoryException {
        try {
            return ArchiveListing.complete(viewsOf(lookup.find().stream().filter(filter).collect(Collectors.toList())));
        } catch (CorruptedRecordException damage) {
            return ArchiveListing.recovered(viewsOf(damage.getRecovered().stream().filter(filter)
                    .collect(Collectors.toList())), damage.getMessage());
        }
    }

    private static List<HighlightView> viewsOf(List<Highlight> highlights) {
        List<Highlight> newestFirst = new ArrayList<>(highlights);
        newestFirst.sort(Comparator.comparing(Highlight::getCapturedAt).reversed());
        return newestFirst.stream()
                .map(HighlightView::from)
                .collect(Collectors.toList());
    }

    @FunctionalInterface
    private interface ArchiveLookup {
        List<Highlight> find() throws RepositoryException;
    }
}
