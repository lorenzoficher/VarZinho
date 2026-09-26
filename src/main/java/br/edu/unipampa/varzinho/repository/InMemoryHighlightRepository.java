package br.edu.unipampa.varzinho.repository;

import br.edu.unipampa.varzinho.domain.highlight.Highlight;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Keeps highlights in memory only, so they vanish with the process. It exists to
 * prove the abstraction holds and to let other aggregates' tests run without a file.
 */
public final class InMemoryHighlightRepository implements HighlightRepository {

    private final Map<String, Highlight> highlightsById = new LinkedHashMap<>();

    @Override
    public void save(Highlight highlight) {
        if (highlight == null) {
            throw new IllegalArgumentException("there is no highlight to save");
        }
        highlightsById.put(highlight.getId(), highlight);
    }

    @Override
    public Optional<Highlight> findById(String id) {
        return Optional.ofNullable(highlightsById.get(id));
    }

    @Override
    public List<Highlight> findAll() {
        return new ArrayList<>(highlightsById.values());
    }

    @Override
    public List<Highlight> findByCourt(int courtNumber) {
        return highlightsById.values().stream()
                .filter(highlight -> highlight.getCourtNumber() == courtNumber)
                .collect(Collectors.toList());
    }
}
