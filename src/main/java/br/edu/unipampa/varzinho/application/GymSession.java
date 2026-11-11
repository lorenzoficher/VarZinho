package br.edu.unipampa.varzinho.application;

import br.edu.unipampa.varzinho.application.port.GymRepository;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.domain.structure.Gym;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * The gym one run of the application works on, and where its changes are kept.
 *
 * <p>It opens on what was saved, or on the fallback gym when nothing was. When what was
 * saved cannot be read, it says so and runs on the fallback gym without keeping it
 * anywhere, so a later save cannot overwrite the damaged file someone may still want to
 * repair.
 */
public final class GymSession {
    private final Gym gym;
    private final GymRepository repository;

    private GymSession(Gym gym, GymRepository repository) {
        this.gym = gym;
        this.repository = repository;
    }

    /**
     * @param fallback the gym to run on when nothing usable was saved
     * @param errorSink told when the saved gym could not be read
     */
    public static GymSession start(GymRepository repository, Gym fallback, Consumer<String> errorSink) {
        if (repository == null) throw new IllegalArgumentException("a session needs somewhere to keep the gym");
        if (fallback == null) throw new IllegalArgumentException("a session needs a gym to fall back on");
        if (errorSink == null) throw new IllegalArgumentException("a session needs somewhere to report errors");
        Optional<Gym> saved;
        try {
            saved = repository.load();
        } catch (RepositoryException damage) {
            errorSink.accept("The saved gym could not be read (" + damage.getMessage()
                    + "). Opening the default gym; changes in this run will not be saved over that file.");
            return new GymSession(fallback, new UnkeptGym());
        }
        // The window needs a court to show; a saved gym without one is as good as none.
        Gym gym = saved.filter(stored -> !stored.getCourts().isEmpty()).orElse(fallback);
        return new GymSession(gym, repository);
    }

    public Gym gym() {
        return gym;
    }

    /**
     * Changes to this session's gym, each kept where the session keeps it: nowhere, when
     * the saved gym could not be read.
     */
    public ManageGym manageGym() {
        return new ManageGym(gym, repository);
    }

    /**
     * Where a session that could not read its file keeps the gym: nowhere. The gym itself
     * lives on in the session for the rest of the run; only the file is left alone.
     */
    private static final class UnkeptGym implements GymRepository {
        @Override
        public void save(Gym gym) {
            // Deliberately kept nowhere: writing it would overwrite the damaged file.
        }

        @Override
        public Optional<Gym> load() {
            return Optional.empty();
        }
    }
}
