package br.edu.unipampa.varzinho.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unipampa.varzinho.domain.capture.StreamCamera;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.enums.Resolution;
import org.junit.jupiter.api.Test;

class ConnectStreamCameraTest {

    private final StreamCamera phone = new StreamCamera("phone-1", "Phone", Resolution.HD, 30,
            "http://192.168.0.42:8080/video", (window, target) -> { });

    @Test
    void thePhoneLandsOnANewCourtAfterTheHighestOneAndIsRecording() {
        Gym gym = arena();
        gym.addCourt(new Court(1));
        gym.addCourt(new Court(4));

        CourtView court = new ConnectStreamCamera(gym, phone).connect();

        assertEquals(5, court.getNumber());
        assertEquals("phone-1", gym.findCourt(5).getCameras().get(0).getId());
        assertEquals(1, gym.findCourt(5).getCameras().size());
        assertTrue(phone.isRecording());
    }

    @Test
    void aGymWithNoCourtsGivesThePhoneCourtOne() {
        CourtView court = new ConnectStreamCamera(arena(), phone).connect();

        assertEquals(1, court.getNumber());
    }

    @Test
    void aDropStopsThePhone() {
        ConnectStreamCamera connection = new ConnectStreamCamera(arena(), phone);
        connection.connect();

        connection.phoneDropped();

        assertFalse(phone.isRecording());
    }

    @Test
    void aSecondDropChangesNothing() {
        Gym gym = arena();
        ConnectStreamCamera connection = new ConnectStreamCamera(gym, phone);
        connection.connect();
        connection.phoneDropped();

        connection.phoneDropped();

        assertFalse(phone.isRecording());
        assertEquals(1, gym.getCourts().size());
        assertEquals(1, gym.findCourt(1).getCameras().size());
    }

    private static Gym arena() {
        return new Gym("Arena", "Rua Central 100");
    }
}
