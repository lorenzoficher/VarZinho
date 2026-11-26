package br.edu.unipampa.varzinho.application;

import br.edu.unipampa.varzinho.domain.structure.Court;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The immutable court data an interface may display: a snapshot of the court and its
 * cameras taken when the view was built.
 */
public final class CourtView {

    private final int number;
    private final List<CameraView> cameras;
    private final int secondsRecorded;
    private final String displayName;
    private final String sport;

    private CourtView(Court court, List<CameraView> cameras) {
        this.number = court.getNumber();
        this.cameras = List.copyOf(cameras);
        this.secondsRecorded = court.secondsRecorded();
        this.displayName = court.getDisplayName().orElse(null);
        this.sport = court.getSport().orElse(null);
    }

    static CourtView from(Court court) {
        List<CameraView> cameras = court.getCameras().stream()
                .map(CameraView::from)
                .collect(Collectors.toList());
        return new CourtView(court, cameras);
    }

    /** {@return the court's number in the gym} */
    public int getNumber() {
        return number;
    }

    /** {@return the name the operator gave the court, or empty when it had none} */
    public Optional<String> getDisplayName() {
        return Optional.ofNullable(displayName);
    }

    /** {@return the sport the operator recorded for the court, or empty when it had none} */
    public Optional<String> getSport() {
        return Optional.ofNullable(sport);
    }

    /** {@return the court's cameras as they were, unmodifiable} */
    public List<CameraView> getCameras() {
        return cameras;
    }

    /** {@return how many cameras the court has} */
    public int getCameraCount() {
        return cameras.size();
    }

    /**
     * Counts this snapshot's recording states, never the live domain objects.
     *
     * @return how many of the court's cameras were recording
     */
    public int getRecordingCameraCount() {
        return (int) cameras.stream().filter(camera -> camera.getStatus().canRecord()).count();
    }

    /**
     * {@return how many seconds a capture could draw on when the snapshot was taken, or 0
     * when no camera was recording}
     */
    public int getSecondsRecorded() {
        return secondsRecorded;
    }

    /** {@return how many seconds a capture on this court keeps} */
    public int getCaptureSeconds() {
        return Court.DEFAULT_CAPTURE_SECONDS;
    }
}
