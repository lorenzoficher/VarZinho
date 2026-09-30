package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.exception.RepositoryException;
import br.edu.unipampa.varzinho.repository.GymRepository;
import br.edu.unipampa.varzinho.repository.InMemoryGymRepository;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * The gym one run of the window works on, and where its changes are kept.
 *
 * <p>It opens on what was saved, or on the sample gym when nothing was. When what was
 * saved cannot be read, it says so and runs on the sample gym in memory, so a later save
 * cannot overwrite the damaged file someone may still want to repair.
 */
final class GymSession {
    private final Gym gym;
    private final GymRepository repository;

    private GymSession(Gym gym, GymRepository repository) {
        this.gym = gym;
        this.repository = repository;
    }

    /**
     * @param errorSink told when the saved gym could not be read
     */
    static GymSession start(GymRepository repository, Consumer<String> errorSink) {
        if (repository == null) throw new IllegalArgumentException("a session needs somewhere to keep the gym");
        if (errorSink == null) throw new IllegalArgumentException("a session needs somewhere to report errors");
        Optional<Gym> saved;
        try {
            saved = repository.load();
        } catch (RepositoryException damage) {
            errorSink.accept("The saved gym could not be read (" + damage.getMessage()
                    + "). Opening the sample gym; changes in this run will not be saved over that file.");
            return new GymSession(SampleGym.build(), new InMemoryGymRepository());
        }
        // The window needs a court to show; a saved gym without one is as good as none.
        Gym gym = saved.filter(stored -> !stored.getCourts().isEmpty()).orElseGet(SampleGym::build);
        return new GymSession(gym, repository);
    }

    Gym gym() {
        return gym;
    }

    /** Stores the gym as it is now, reporting instead of throwing when that is refused. */
    void save(Consumer<String> errorSink) {
        try {
            repository.save(gym);
        } catch (IllegalArgumentException | RepositoryException refusal) {
            errorSink.accept("The gym could not be saved: " + refusal.getMessage());
        }
    }
}
