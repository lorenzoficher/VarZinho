package br.edu.unipampa.varzinho.application;

import br.edu.unipampa.varzinho.application.port.GymRepository;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.domain.capture.Aimable;
import br.edu.unipampa.varzinho.domain.capture.Camera;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
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

    public CourtView startRecording(int courtNumber, String cameraId) throws RepositoryException {
        return changeCamera(courtNumber, cameraId, Camera::startRecording);
    }

    public CourtView stopRecording(int courtNumber, String cameraId) throws RepositoryException {
        return changeCamera(courtNumber, cameraId, Camera::stopRecording);
    }

    public CourtView sendToMaintenance(int courtNumber, String cameraId)
            throws RepositoryException {
        return changeCamera(courtNumber, cameraId, Camera::sendToMaintenance);
    }

    public CourtView removeCamera(int courtNumber, String cameraId) throws RepositoryException {
        Court court = requireCourt(courtNumber);
        court.removeCamera(requireCamera(court, cameraId));
        repository.save(gym);
        return CourtView.from(court);
    }

    public CourtView moveCamera(int fromCourtNumber, String cameraId, int toCourtNumber)
            throws RepositoryException {
        Court from = requireCourt(fromCourtNumber);
        gym.moveCamera(requireCamera(from, cameraId), toCourtNumber);
        repository.save(gym);
        return CourtView.from(from);
    }

    public CourtView aimCamera(int courtNumber, String cameraId, int pan, int tilt, int zoom)
            throws RepositoryException {
        Court court = requireCourt(courtNumber);
        Camera camera = requireCamera(court, cameraId);
        if (!(camera instanceof Aimable)) {
            throw new IllegalArgumentException("camera " + cameraId + " cannot be aimed");
        }
        ((Aimable) camera).moveTo(pan, tilt, zoom);
        repository.save(gym);
        return CourtView.from(court);
    }

    private CourtView changeCamera(int courtNumber, String cameraId, Consumer<Camera> change)
            throws RepositoryException {
        Court court = requireCourt(courtNumber);
        change.accept(requireCamera(court, cameraId));
        repository.save(gym);
        return CourtView.from(court);
    }

    private static Camera requireCamera(Court court, String cameraId) {
        return court.getCameras().stream()
                .filter(camera -> camera.getId().equals(cameraId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "camera " + cameraId + " is not installed on court " + court.getNumber()));
    }

    private Court requireCourt(int courtNumber) {
        Court court = gym.findCourt(courtNumber);
        if (court == null) {
            throw new IllegalArgumentException("court " + courtNumber + " is not registered");
        }
        return court;
    }
}
