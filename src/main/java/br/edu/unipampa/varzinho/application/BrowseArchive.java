package br.edu.unipampa.varzinho.application;

import br.edu.unipampa.varzinho.application.port.CorruptedRecordException;
import br.edu.unipampa.varzinho.application.port.HighlightRepository;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.domain.highlight.Highlight;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Lists saved highlights without exposing persistence or domain objects to the caller. */
public final class BrowseArchive {

    private final HighlightRepository repository;

    public BrowseArchive(HighlightRepository repository) {
        if (repository == null) {
            throw new IllegalArgumentException("browsing the archive needs a highlight repository");
        }
        this.repository = repository;
    }

    public ArchiveListing all() throws RepositoryException {
        return browse(repository::findAll);
    }

    public ArchiveListing byCourt(int courtNumber) throws RepositoryException {
        return browse(() -> repository.findByCourt(courtNumber));
    }

    private static ArchiveListing browse(ArchiveLookup lookup) throws RepositoryException {
        try {
            return ArchiveListing.complete(viewsOf(lookup.find()));
        } catch (CorruptedRecordException damage) {
            return ArchiveListing.recovered(viewsOf(damage.getRecovered()), damage.getMessage());
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
