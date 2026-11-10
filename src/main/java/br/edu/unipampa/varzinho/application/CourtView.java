package br.edu.unipampa.varzinho.application;

import br.edu.unipampa.varzinho.domain.structure.Court;

import java.util.List;
import java.util.stream.Collectors;

/** The immutable court data an interface may display. */
public final class CourtView {

    private final int number;
    private final List<CameraView> cameras;
    private final int secondsRecorded;

    private CourtView(int number, List<CameraView> cameras, int secondsRecorded) {
        this.number = number;
        this.cameras = List.copyOf(cameras);
        this.secondsRecorded = secondsRecorded;
    }

    static CourtView from(Court court) {
        List<CameraView> cameras = court.getCameras().stream()
                .map(CameraView::from)
                .collect(Collectors.toList());
        return new CourtView(court.getNumber(), cameras, court.secondsRecorded());
    }

    public int getNumber() {
        return number;
    }

    public List<CameraView> getCameras() {
        return cameras;
    }

    public int getSecondsRecorded() {
        return secondsRecorded;
    }
}
