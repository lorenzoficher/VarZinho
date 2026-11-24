package br.edu.unipampa.varzinho.application.port;

import br.edu.unipampa.varzinho.domain.people.Operator;

import java.util.Optional;

/** Where operator accounts live, independently from the gym's structure. */
public interface OperatorStore {

    /**
     * Stores a new operator.
     *
     * @param operator the account to add
     * @throws IllegalArgumentException if the operator is missing, its username already
     *     exists regardless of case, or a text cannot be represented by every adapter
     * @throws RepositoryException if the storage cannot be read or written
     */
    void save(Operator operator) throws RepositoryException;

    /**
     * Puts this operator in place of the stored one with the same username — how a new
     * password reaches the store, since an {@link Operator} never changes.
     *
     * @param operator the account that takes the stored one's place
     * @throws IllegalArgumentException if the operator is missing, no stored operator has
     *     its username, or a text cannot be represented by every adapter
     * @throws RepositoryException if the storage cannot be read or written
     */
    void replace(Operator operator) throws RepositoryException;

    /**
     * Looks up one operator account. An unknown or missing username gives an empty result.
     *
     * @param username the username to look for
     * @return the stored operator, or empty when none has this username
     * @throws RepositoryException if the storage cannot be read
     */
    Optional<Operator> findByUsername(String username) throws RepositoryException;

    /**
     * Tells whether the first operator still has to be created.
     *
     * @return whether at least one operator account exists
     * @throws RepositoryException if the storage cannot be read
     */
    boolean hasAny() throws RepositoryException;
}
