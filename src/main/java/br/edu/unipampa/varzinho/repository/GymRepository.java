package br.edu.unipampa.varzinho.repository;

import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.exception.RepositoryException;

import java.util.Optional;

/**
 * Where the gym's structure lives between executions: its courts, the cameras on each
 * and the state each camera was left in.
 *
 * <p>Highlights are not part of it; they have their own {@link HighlightRepository}. People
 * are not either, because the window never registers any.
 */
public interface GymRepository {

    /**
     * Stores the gym as it is now, replacing whatever was stored before.
     *
     * @throws IllegalArgumentException if a name, address, id or model holds a value the
     *     storage cannot represent; nothing is written
     * @throws RepositoryException if the storage cannot be written
     */
    void save(Gym gym) throws RepositoryException;

    /**
     * @return the stored gym, or empty when nothing has been stored yet
     * @throws RepositoryException if what is stored cannot be read back as a gym
     */
    Optional<Gym> load() throws RepositoryException;
}
