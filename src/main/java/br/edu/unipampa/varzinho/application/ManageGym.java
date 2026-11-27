package br.edu.unipampa.varzinho.application;

import br.edu.unipampa.varzinho.application.port.GymRepository;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.domain.capture.Aimable;
import br.edu.unipampa.varzinho.domain.capture.Camera;
import br.edu.unipampa.varzinho.domain.capture.PtzCamera;
import br.edu.unipampa.varzinho.enums.CameraStatus;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Changes the gym's structure and exposes only immutable views of it.
 *
 * <p>Every change is applied to the gym first and stored after. When storing fails the
 * change is undone before the {@link RepositoryException} reaches the caller, so the window
 * never shows a court or a camera that a restart would take away. A value the storage
 * cannot hold is reported the same way, as a failure to keep the change, never as a refusal.
 */
public final class ManageGym {

    private final Gym gym;
    private final GymRepository repository;

    /**
     * Manages a gym, storing it after every change.
     *
     * @param gym the gym every change is applied to, in memory first
     * @param repository where the gym is stored after each change
     * @throws IllegalArgumentException if either is missing
     */
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

    /** {@return every court of the gym, by number, as views that do not change} */
    public List<CourtView> courts() {
        return List.copyOf(gym.getCourts().stream()
                .sorted(Comparator.comparingInt(Court::getNumber))
                .map(CourtView::from)
                .collect(Collectors.toList()));
    }

    /**
     * Shows one court.
     *
     * @param courtNumber the court to show
     * @return a view of that court as it is now
     * @throws IllegalArgumentException if the gym has no court with that number
     */
    public CourtView court(int courtNumber) {
        return CourtView.from(requireCourt(courtNumber));
    }

    /**
     * Adds an empty court and stores the gym.
     *
     * @param courtNumber the new court's number
     * @return a view of the new court
     * @throws IllegalArgumentException if the number is not positive or already taken;
     *     nothing changes
     * @throws RepositoryException if the gym cannot be stored; nothing changes
     */
    public CourtView addCourt(int courtNumber) throws RepositoryException {
        Court court = new Court(courtNumber);
        gym.addCourt(court);
        keepOrUndo(() -> gym.removeCourt(courtNumber));
        return CourtView.from(court);
    }

    /**
     * Takes an empty court out of the gym and stores the gym. The window always works on
     * a selected court, so the only court is kept even though a {@link Gym} may be empty.
     *
     * @param courtNumber the court to remove
     * @throws IllegalArgumentException if the gym has no court with that number
     * @throws IllegalStateException if it is the only court or still holds a camera;
     *     nothing is removed or stored
     * @throws RepositoryException if the gym cannot be stored; nothing changes
     */
    public void removeCourt(int courtNumber) throws RepositoryException {
        Court court = requireCourt(courtNumber);
        if (gym.getCourts().size() == 1) {
            throw new IllegalStateException("court " + courtNumber + " is the only court");
        }
        gym.removeCourt(courtNumber);
        keepOrUndo(() -> gym.addCourt(court));
    }

    /**
     * Builds the camera a specification describes, installs it on a court and stores the gym.
     *
     * @param courtNumber the court that receives the camera
     * @param spec what to build
     * @return a view of the court with its new camera
     * @throws IllegalArgumentException if the specification is missing or the gym has no
     *     court with that number
     * @throws br.edu.unipampa.varzinho.exception.DuplicateCameraException if a camera with
     *     the same id is already installed anywhere in the gym
     * @throws RepositoryException if the gym cannot be stored; nothing changes
     */
    public CourtView installCamera(int courtNumber, CameraSpec spec) throws RepositoryException {
        if (spec == null) {
            throw new IllegalArgumentException("installing a camera needs its specification");
        }
        install(courtNumber, spec.getKind().build(spec));
        return court(courtNumber);
    }

    /**
     * Starts a camera recording and stores the gym.
     *
     * @param courtNumber the court the camera is on
     * @param cameraId the camera to change
     * @return a view of the court after the change
     * @throws IllegalArgumentException if the court is unknown or the camera is not on it
     * @throws IllegalStateException if the camera is under maintenance
     * @throws RepositoryException if the gym cannot be stored; nothing changes
     */
    public CourtView startRecording(int courtNumber, String cameraId) throws RepositoryException {
        return changeCamera(courtNumber, cameraId, Camera::startRecording);
    }

    /**
     * Installs a simulated PTZ camera already aimed, under the first free id of the form
     * {@code ptz-<court>} or {@code ptz-<court>-<n>}, and stores the gym. It is not recording.
     *
     * @param courtNumber the court that receives the camera
     * @param pan the horizontal angle to aim at
     * @param tilt the vertical angle to aim at
     * @param zoom the zoom level to aim at
     * @throws IllegalArgumentException if the gym has no court with that number, or a
     *     coordinate is outside its range; nothing is installed
     * @throws RepositoryException if the gym cannot be stored; nothing changes
     */
    public void installSimulatedPtz(int courtNumber, int pan, int tilt, int zoom) throws RepositoryException {
        String prefix = "ptz-" + courtNumber;
        String id = prefix;
        var installed = gym.getCourts().stream().flatMap(court -> court.getCameras().stream())
                .map(Camera::getId).collect(Collectors.toSet());
        for (int suffix = 2; installed.contains(id); suffix++) id = prefix + "-" + suffix;
        var camera = new PtzCamera(id, "PTZ", Resolution.HD, Court.DEFAULT_CAPTURE_SECONDS);
        camera.moveTo(pan, tilt, zoom);
        install(courtNumber, camera);
    }

    /**
     * Stops a camera recording and stores the gym.
     *
     * @param courtNumber the court the camera is on
     * @param cameraId the camera to change
     * @return a view of the court after the change
     * @throws IllegalArgumentException if the court is unknown or the camera is not on it
     * @throws IllegalStateException if the camera is under maintenance
     * @throws RepositoryException if the gym cannot be stored; nothing changes
     */
    public CourtView stopRecording(int courtNumber, String cameraId) throws RepositoryException {
        return changeCamera(courtNumber, cameraId, Camera::stopRecording);
    }

    /**
     * Takes a camera out of service, stopping it if it was recording, and stores the gym.
     *
     * @param courtNumber the court the camera is on
     * @param cameraId the camera to change
     * @return a view of the court after the change
     * @throws IllegalArgumentException if the court is unknown or the camera is not on it
     * @throws RepositoryException if the gym cannot be stored; nothing changes
     */
    public CourtView sendToMaintenance(int courtNumber, String cameraId)
            throws RepositoryException {
        return changeCamera(courtNumber, cameraId, Camera::sendToMaintenance);
    }

    /**
     * Returns a camera from maintenance, stopped, and stores the gym.
     *
     * @param courtNumber the court the camera is on
     * @param cameraId the camera to change
     * @return a view of the court after the change
     * @throws IllegalArgumentException if the court is unknown or the camera is not on it
     * @throws IllegalStateException if the camera is not under maintenance
     * @throws RepositoryException if the gym cannot be stored; nothing changes
     */
    public CourtView releaseFromMaintenance(int courtNumber, String cameraId) throws RepositoryException {
        return changeCamera(courtNumber, cameraId, Camera::releaseFromMaintenance);
    }

    /**
     * Uninstalls a camera and stores the gym.
     *
     * @param courtNumber the court the camera is on
     * @param cameraId the camera to remove
     * @return a view of the court without it
     * @throws IllegalArgumentException if the court is unknown or the camera is not on it
     * @throws RepositoryException if the gym cannot be stored; nothing changes
     */
    public CourtView removeCamera(int courtNumber, String cameraId) throws RepositoryException {
        Court court = requireCourt(courtNumber);
        Camera camera = requireCamera(court, cameraId);
        court.removeCamera(camera);
        keepOrUndo(() -> court.installCamera(camera));
        return CourtView.from(court);
    }

    /**
     * Moves a camera to another court and stores the gym. A recording camera arrives
     * switched off, so no highlight on its new court holds the old court's footage.
     *
     * @param fromCourtNumber the court the camera is on now
     * @param cameraId the camera to move
     * @param toCourtNumber the court it goes to
     * @return a view of the court it left
     * @throws IllegalArgumentException if either court is unknown, the camera is not on the
     *     first one, or both are the same court; nothing moves
     * @throws RepositoryException if the gym cannot be stored; nothing changes
     */
    public CourtView moveCamera(int fromCourtNumber, String cameraId, int toCourtNumber)
            throws RepositoryException {
        Court from = requireCourt(fromCourtNumber);
        Camera camera = requireCamera(from, cameraId);
        CameraStatus before = camera.getStatus();
        gym.moveCamera(camera, toCourtNumber);
        keepOrUndo(() -> {
            gym.moveCamera(camera, fromCourtNumber);
            restoreStatus(camera, before);
        });
        return CourtView.from(from);
    }

    /**
     * Points a camera that can be aimed and stores the gym.
     *
     * @param courtNumber the court the camera is on
     * @param cameraId the camera to aim
     * @param pan the horizontal angle
     * @param tilt the vertical angle
     * @param zoom the zoom level
     * @return a view of the court after the change
     * @throws IllegalArgumentException if the court is unknown, the camera is not on it,
     *     cannot be aimed, or a coordinate is outside its range
     * @throws RepositoryException if the gym cannot be stored; nothing changes
     */
    public CourtView aimCamera(int courtNumber, String cameraId, int pan, int tilt, int zoom)
            throws RepositoryException {
        Court court = requireCourt(courtNumber);
        Camera camera = requireCamera(court, cameraId);
        if (!(camera instanceof Aimable aimable)) {
            throw new IllegalArgumentException("camera " + cameraId + " cannot be aimed");
        }
        CameraView before = CameraView.from(camera);
        aimable.moveTo(pan, tilt, zoom);
        keepOrUndo(() -> aimable.moveTo(before.getPan().orElseThrow(),
                before.getTilt().orElseThrow(), before.getZoom().orElseThrow()));
        return CourtView.from(court);
    }

    /**
     * Names a court for the people who use it and stores the gym. A blank name clears it.
     *
     * @param courtNumber the court to name
     * @param displayName the new name
     * @return a view of the court with its new name
     * @throws IllegalArgumentException if the court is unknown or {@link Court#rename}
     *     refuses the name; nothing changes
     * @throws RepositoryException if the gym cannot be stored; nothing changes
     */
    public CourtView renameCourt(int courtNumber, String displayName) throws RepositoryException {
        return relabel(courtNumber, displayName, Court::getDisplayName, Court::rename);
    }

    /**
     * Records the sport played on a court, as the operator calls it, and stores the gym.
     * A blank sport clears it.
     *
     * @param courtNumber the court to change
     * @param sport the new sport
     * @return a view of the court with its new sport
     * @throws IllegalArgumentException if the court is unknown or {@link Court#assignSport}
     *     refuses the sport; nothing changes
     * @throws RepositoryException if the gym cannot be stored; nothing changes
     */
    public CourtView chooseSport(int courtNumber, String sport) throws RepositoryException {
        return relabel(courtNumber, sport, Court::getSport, Court::assignSport);
    }

    private CourtView relabel(int courtNumber, String label, Function<Court, Optional<String>> current,
            BiConsumer<Court, String> change) throws RepositoryException {
        Court court = requireCourt(courtNumber);
        String previous = current.apply(court).orElse("");
        change.accept(court, label);
        keepOrUndo(() -> change.accept(court, previous));
        return CourtView.from(court);
    }

    private CourtView changeCamera(int courtNumber, String cameraId, Consumer<Camera> change)
            throws RepositoryException {
        Court court = requireCourt(courtNumber);
        Camera camera = requireCamera(court, cameraId);
        CameraStatus before = camera.getStatus();
        change.accept(camera);
        keepOrUndo(() -> restoreStatus(camera, before));
        return CourtView.from(court);
    }

    private void install(int courtNumber, Camera camera) throws RepositoryException {
        gym.installCamera(courtNumber, camera);
        keepOrUndo(() -> requireCourt(courtNumber).removeCamera(camera));
    }

    /**
     * Puts a camera back in a status it had. One that goes back to recording starts on an
     * empty buffer, as every start does: footage from before the failed change is not
     * passed off as the present.
     */
    private static void restoreStatus(Camera camera, CameraStatus previous) {
        if (camera.getStatus() == previous) {
            return;
        }
        if (camera.getStatus() == CameraStatus.MAINTENANCE) {
            camera.releaseFromMaintenance();
        }
        switch (previous) {
            case ACTIVE:
                camera.startRecording();
                break;
            case MAINTENANCE:
                camera.sendToMaintenance();
                break;
            default:
                camera.stopRecording();
        }
    }

    /**
     * Stores the gym after a change it already took, or takes the change back when it
     * cannot be stored, so memory never runs ahead of what a restart would load.
     */
    private void keepOrUndo(Runnable undo) throws RepositoryException {
        try {
            keep();
        } catch (RepositoryException | RuntimeException notKept) {
            undo.run();
            throw notKept;
        }
    }

    /**
     * Stores the gym. A value the storage cannot hold is a failure to keep the change,
     * not a refusal of it, so it is reported as one.
     */
    private void keep() throws RepositoryException {
        try {
            repository.save(gym);
        } catch (IllegalArgumentException unstorable) {
            throw new RepositoryException(unstorable.getMessage(), unstorable);
        }
    }

    private static Camera requireCamera(Court court, String cameraId) {
        return court.getCameras().stream()
                .filter(camera -> camera.getId().equals(cameraId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "camera " + cameraId + " is not installed on court " + court.getNumber()));
    }

    private Court requireCourt(int courtNumber) {
        return gym.findCourt(courtNumber).orElseThrow(
                () -> new IllegalArgumentException("court " + courtNumber + " is not registered"));
    }
}
