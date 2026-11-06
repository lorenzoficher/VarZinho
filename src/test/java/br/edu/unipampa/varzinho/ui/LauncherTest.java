package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.domain.capture.Camera;
import br.edu.unipampa.varzinho.domain.capture.FixedCamera;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.enums.Resolution;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LauncherTest {
    private final Gym gym = new Gym("Arena", "100 Sports Avenue");
    private final Camera phone = new FixedCamera("phone-1", "Phone", Resolution.HD, 30, 0);

    @Test
    void phoneGetsCourtOneInAnEmptyGym() {
        Court court = Launcher.givePhoneItsCourt(gym, phone);

        assertEquals(1, court.getNumber());
        assertSame(court, gym.findCourt(1));
    }

    @Test
    void phoneGetsTheCourtAfterTheHighestNumberedOne() {
        gym.addCourt(new Court(1));
        gym.addCourt(new Court(3));

        Court court = Launcher.givePhoneItsCourt(gym, phone);

        assertEquals(4, court.getNumber());
    }

    @Test
    void phoneIsInstalledOnItsCourtAndRecording() {
        Court court = Launcher.givePhoneItsCourt(gym, phone);

        assertTrue(court.hasCamera("phone-1"));
        assertTrue(phone.isRecording());
    }
}
