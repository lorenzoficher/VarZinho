package br.edu.unipampa.varzinho.application;

import br.edu.unipampa.varzinho.application.port.GymRepository;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Changes the gym's structure and exposes only immutable views of it. */
public final class ManageGym {

    private final Gym gym;
    private final GymRepository repository;

    public ManageGym(Gym gym, GymRepository repository) {
        if (gym == null) {
            throw new IllegalArgumentException("gym management needs a gym");
        }
        if (repository == null) {
            throw new IllegalArgumentException("gym management needs a gym repository");
        }
        this.gym = gym;
        this.repository = repository;
    }

    public List<CourtView> courts() {
        return List.copyOf(gym.getCourts().stream()
                .sorted(Comparator.comparingInt(Court::getNumber))
                .map(CourtView::from)
                .collect(Collectors.toList()));
    }

    public CourtView court(int courtNumber) {
        return CourtView.from(requireCourt(courtNumber));
    }

    public CourtView addCourt(int courtNumber) throws RepositoryException {
        Court court = new Court(courtNumber);
        gym.addCourt(court);
        repository.save(gym);
        return CourtView.from(court);
    }

    public CourtView installCamera(int courtNumber, CameraSpec spec) throws RepositoryException {
        if (spec == null) {
            throw new IllegalArgumentException("installing a camera needs its specification");
        }
        gym.installCamera(courtNumber, spec.getKind().build(spec));
        repository.save(gym);
        return court(courtNumber);
    }

    private Court requireCourt(int courtNumber) {
        Court court = gym.findCourt(courtNumber);
        if (court == null) {
            throw new IllegalArgumentException("court " + courtNumber + " is not registered");
        }
        return court;
    }
}
