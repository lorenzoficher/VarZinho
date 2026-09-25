package br.edu.unipampa.varzinho.domain.structure;

import br.edu.unipampa.varzinho.domain.people.Athlete;
import br.edu.unipampa.varzinho.domain.people.Operator;
import br.edu.unipampa.varzinho.domain.people.Person;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GymTest {
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
