package br.edu.unipampa.varzinho.infrastructure.persistence;

import br.edu.unipampa.varzinho.application.port.GymRepository;
import br.edu.unipampa.varzinho.domain.structure.Gym;

import java.util.Optional;

/**
 * Keeps the gym in memory only, so it vanishes with the process.
 *
 * <p>It lets the use cases that save the gym be tested without a file.
 */
public final class InMemoryGymRepository implements GymRepository {

    private Gym stored;

    @Override
    public void save(Gym gym) {
        if (gym == null) {
            throw new IllegalArgumentException("there is no gym to save");
        }
        stored = gym;
    }

    @Override
    public Optional<Gym> load() {
        return Optional.ofNullable(stored);
    }
}
