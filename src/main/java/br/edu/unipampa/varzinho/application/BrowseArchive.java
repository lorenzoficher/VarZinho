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

    /**
     * Browses with the machine's clock, taking the machine's zone as the gym's.
     *
     * @param repository where the highlights are kept
     * @throws IllegalArgumentException if the repository is missing
     */
    public BrowseArchive(HighlightRepository repository) {
        this(repository, Clock.systemDefaultZone(), ZoneId.systemDefault());
    }

    /**
     * Browses with a given clock and zone, so "today" is the gym's day and tests can fix it.
     *
     * @param repository where the highlights are kept
     * @param clock what "today" is read from
     * @param zone the gym's local zone; it can differ from the clock's own zone
     * @throws IllegalArgumentException if any of the three is missing
     */
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

    /**
     * Lists every highlight, newest first.
     *
     * @return the highlights that could be read, with a warning when some could not
     * @throws RepositoryException if the archive cannot be read at all; a damaged line is
     *     not one, it becomes the listing's recovery warning
     */
    public ArchiveListing all() throws RepositoryException {
        return browse(repository::findAll);
    }

    /**
     * Lists one court's highlights, newest first.
     *
     * @param courtNumber the court to list; an unknown court gives an empty listing
     * @return that court's highlights that could be read, with a warning when some could not
     * @throws RepositoryException if the archive cannot be read at all; a damaged line is
     *     not one, it becomes the listing's recovery warning
     */
    public ArchiveListing byCourt(int courtNumber) throws RepositoryException {
        return browse(() -> repository.findByCourt(courtNumber));
    }

    /**
     * Lists one local calendar day, newest first, retaining any recovery warning.
     *
     * @param day the day in the gym's zone
     * @return that day's highlights that could be read
     * @throws IllegalArgumentException if the day is missing
     * @throws RepositoryException if the archive cannot be read at all; a damaged line is
     *     not one, it becomes the listing's recovery warning
     */
    public ArchiveListing byDay(LocalDate day) throws RepositoryException {
        return browse(repository::findAll, capturedOn(day));
    }

    /**
     * Lists that local day on one court, newest first, retaining any recovery warning.
     *
     * @param day the day in the gym's zone
     * @param courtNumber the court to list
     * @return that day's highlights on that court that could be read
     * @throws IllegalArgumentException if the day is missing
     * @throws RepositoryException if the archive cannot be read at all; a damaged line is
     *     not one, it becomes the listing's recovery warning
     */
    public ArchiveListing byDay(LocalDate day, int courtNumber) throws RepositoryException {
        return browse(() -> repository.findByCourt(courtNumber), capturedOn(day));
    }

    /**
     * Counts today's readable highlights in the gym's zone and reports the latest one.
     *
     * @return today's count and latest highlight, with any recovery warning
     * @throws RepositoryException if the archive cannot be read at all; a damaged line is
     *     not one, it becomes the listing's recovery warning
     */
    public ArchiveSummary todaySummary() throws RepositoryException {
        return ArchiveSummary.from(byDay(LocalDate.now(clock.withZone(zone))));
    }

    /**
     * Counts today's readable highlights for one court, retaining any recovery warning.
     *
     * @param courtNumber the court to count
     * @return today's count and latest highlight on that court
     * @throws RepositoryException if the archive cannot be read at all; a damaged line is
     *     not one, it becomes the listing's recovery warning
     */
    public ArchiveSummary todaySummary(int courtNumber) throws RepositoryException {
        return ArchiveSummary.from(byDay(LocalDate.now(clock.withZone(zone)), courtNumber));
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
