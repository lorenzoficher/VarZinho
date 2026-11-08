package br.edu.unipampa.varzinho.application;

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
        return listing(repository.findAll());
    }

    public ArchiveListing byCourt(int courtNumber) throws RepositoryException {
        return listing(repository.findByCourt(courtNumber));
    }

    private static ArchiveListing listing(List<Highlight> highlights) {
        List<Highlight> newestFirst = new ArrayList<>(highlights);
        newestFirst.sort(Comparator.comparing(Highlight::getCapturedAt).reversed());
        return ArchiveListing.complete(newestFirst.stream()
                .map(HighlightView::from)
                .collect(Collectors.toList()));
    }
}
