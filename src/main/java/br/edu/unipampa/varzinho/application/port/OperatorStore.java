package br.edu.unipampa.varzinho.application.port;

import br.edu.unipampa.varzinho.domain.people.Operator;

import java.util.Optional;

/** Where operator accounts live, independently from the gym's structure. */
public interface OperatorStore {
    /**
     * Stores a new operator.
     *
     * @throws IllegalArgumentException if the operator is missing, its username already
     *     exists regardless of case, or a text cannot be represented by every adapter
     * @throws RepositoryException if the storage cannot be read or written
     */
    void save(Operator operator) throws RepositoryException;

    /**
     * Puts this operator in place of the stored one with the same username — how a new
     * password reaches the store, since an {@link Operator} never changes.
     *
     * @throws IllegalArgumentException if the operator is missing, no stored operator has
     *     its username, or a text cannot be represented by every adapter
     * @throws RepositoryException if the storage cannot be read or written
     */
    void replace(Operator operator) throws RepositoryException;

    /** An unknown or missing username gives an empty result. */
    Optional<Operator> findByUsername(String username) throws RepositoryException;

    /** @return whether at least one operator account exists */
    boolean hasAny() throws RepositoryException;
}
