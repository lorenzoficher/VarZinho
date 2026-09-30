package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.domain.capture.FixedCamera;
import br.edu.unipampa.varzinho.domain.capture.PtzCamera;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.enums.Resolution;

/**
 * The gym the window opens on, so a demonstration needs no typing before the first
 * capture.
 *
 * <p>It only assembles objects through the domain API; every rule stays in the domain.
 */
public final class SampleGym {
    private SampleGym() { }

    /**
     * Builds a gym with two courts: court 1 holds a fixed and a PTZ camera, court 2 a
     * single fixed camera. Every camera is already recording.
     */
    public static Gym build() {
        Gym gym = new Gym("VarZinho Arena", "100 Sports Avenue");

        FixedCamera mainFixed = new FixedCamera("fixed-1", "Fixed Pro", Resolution.FULL_HD, 30, 45);
        PtzCamera mainPtz = new PtzCamera("ptz-1", "PTZ Pro", Resolution.HD, 30);
        Court mainCourt = new Court(1);
        gym.addCourt(mainCourt);
        gym.installCamera(1, mainFixed);
        gym.installCamera(1, mainPtz);

        FixedCamera sideFixed = new FixedCamera("fixed-2", "Fixed Pro", Resolution.FULL_HD, 30, 45);
        Court sideCourt = new Court(2);
        gym.addCourt(sideCourt);
        gym.installCamera(2, sideFixed);

        mainFixed.startRecording();
        mainPtz.startRecording();
        sideFixed.startRecording();
        return gym;
    }
}
