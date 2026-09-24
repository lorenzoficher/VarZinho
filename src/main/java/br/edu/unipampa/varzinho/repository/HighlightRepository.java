package br.edu.unipampa.varzinho.repository;

import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.exception.RepositoryException;

import java.util.List;
import java.util.Optional;

public interface HighlightRepository {
    void save(Highlight highlight) throws RepositoryException;
    Optional<Highlight> findById(String id) throws RepositoryException;
    List<Highlight> findAll() throws RepositoryException;
    List<Highlight> findByCourt(int courtNumber) throws RepositoryException;
}
