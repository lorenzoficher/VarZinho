package br.edu.unipampa.varzinho.application;

import br.edu.unipampa.varzinho.domain.capture.StreamCamera;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;

/**
 * Brings the phone into the gym on a court of its own, and takes it out of recording
 * when its stream drops.
 *
 * <p>The phone gets a new court so a capture there always comes from the phone: a court
 * captures from its first recording camera, and on a shared court that would be
 * whichever camera was installed first. Nothing is saved here — the phone's court
 * exists for one run, while its stream is up.
 */
public final class ConnectStreamCamera {

    private final Gym gym;
    private final StreamCamera phone;

    public ConnectStreamCamera(Gym gym, StreamCamera phone) {
        if (gym == null) {
            throw new IllegalArgumentException("connecting a phone needs a gym");
        }
        if (phone == null) {
            throw new IllegalArgumentException("connecting a phone needs the phone's camera");
        }
        this.gym = gym;
        this.phone = phone;
    }

    /**
     * Opens the court after the gym's highest-numbered one, installs the phone there and
     * starts it recording.
     *
     * @return the phone's new court
     * @throws br.edu.unipampa.varzinho.exception.DuplicateCameraException if the phone is
     *         already installed in the gym
     */
    public CourtView connect() {
        Court court = gym.openNextCourt();
        gym.installCamera(court.getNumber(), phone);
        phone.startRecording();
        return CourtView.from(court);
    }

    /**
     * Stops the phone if it is still recording. The stream may report its drop more than
     * once; only the first report finds the phone recording.
     */
    public void phoneDropped() {
        if (phone.isRecording()) {
            phone.stopRecording();
        }
    }
}
