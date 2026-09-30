package br.edu.unipampa.varzinho.repository;

import br.edu.unipampa.varzinho.domain.structure.Gym;

import java.util.Optional;

/**
 * Keeps the gym in memory only, so it vanishes with the process.
 *
 * <p>The window falls back to it when the gym file is damaged: the session goes on, and
 * the damaged file is left untouched for someone to repair instead of being overwritten.
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
