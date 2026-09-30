package br.edu.unipampa.varzinho.domain.structure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.edu.unipampa.varzinho.domain.capture.FixedCamera;
import br.edu.unipampa.varzinho.domain.people.Athlete;
import br.edu.unipampa.varzinho.domain.people.Operator;
import br.edu.unipampa.varzinho.domain.people.Person;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.exception.DuplicateCameraException;

class GymTest {

    @Test
    void newGymIsEmpty() {
        Gym gym = new Gym("Arena", "Rua A");
        assertTrue(gym.getCourts().isEmpty());
    }

    @Test
    void findAddedCourt() {
        Gym gym = new Gym("Arena", "Rua A");
        Court court = new Court(1);
        gym.addCourt(court);
        assertSame(court, gym.findCourt(1));
    }

    @Test
    void findUnknownCourt() {
        Gym gym = new Gym("Arena", "Rua A");
        assertNull(gym.findCourt(99));
    }

    @Test
    void rejectDuplicateCourt() {
        Gym gym = new Gym("Arena", "Rua A");
        gym.addCourt(new Court(1));

        assertThrows(
            IllegalArgumentException.class,
            () -> gym.addCourt(new Court(1))
        );
    }

    @Test
    void installsACameraOnTheNamedCourt() {
        Gym gym = new Gym("Arena", "Rua A");
        gym.addCourt(new Court(1));
        FixedCamera camera = new FixedCamera("fixed-1", "Fixed", Resolution.HD, 30, 45);

        gym.installCamera(1, camera);

        assertEquals(List.of(camera), gym.findCourt(1).getCameras());
    }

    @Test
    void refusesACameraIdAlreadyInstalledOnAnotherCourt() {
        Gym gym = new Gym("Arena", "Rua A");
        gym.addCourt(new Court(1));
        gym.addCourt(new Court(2));
        gym.installCamera(1, new FixedCamera("fixed-1", "Fixed", Resolution.HD, 30, 45));

        DuplicateCameraException refusal = assertThrows(DuplicateCameraException.class,
                () -> gym.installCamera(2, new FixedCamera("fixed-1", "Fixed", Resolution.HD, 30, 45)));

        assertEquals("camera fixed-1 is already installed on court 1", refusal.getMessage());
        assertTrue(gym.findCourt(2).getCameras().isEmpty());
    }

    @Test
    void refusesToInstallOnACourtTheGymDoesNotHave() {
        Gym gym = new Gym("Arena", "Rua A");

        assertThrows(IllegalArgumentException.class,
                () -> gym.installCamera(9, new FixedCamera("fixed-1", "Fixed", Resolution.HD, 30, 45)));
    }

    @Test
    void courtsAreUnmodifiable() {
        Gym gym = new Gym("Arena", "Rua A");
        gym.addCourt(new Court(1));

        List<Court> courts = gym.getCourts();

        assertThrows(
            UnsupportedOperationException.class,
            () -> courts.clear()
        );

        assertEquals(1, gym.getCourts().size());
    }

    @Test
    void registersDifferentKindsOfPeopleThroughTheSameOperation() {
        Gym gym = new Gym("VarZinho", "100 Sports Avenue");
        Person athlete = new Athlete("Ana", "ATH-1", LocalDate.of(2000, 1, 1), 10, "Forward");
        Person operator = new Operator("Otavio", "OP-1", LocalDate.of(1990, 1, 1), "B-7", "Evening");

        gym.registerPerson(athlete);
        gym.registerPerson(operator);

        assertEquals(athlete, gym.findPerson("ATH-1").orElseThrow());
        assertEquals(operator, gym.findPerson("OP-1").orElseThrow());
        assertTrue(gym.findPerson("unknown").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> gym.registerPerson(athlete));
        assertThrows(UnsupportedOperationException.class, () -> gym.getPeople().clear());
    }
}
