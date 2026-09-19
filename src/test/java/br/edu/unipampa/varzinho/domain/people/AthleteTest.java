package br.edu.unipampa.varzinho.domain.people;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class AthleteTest {

    @Test
    void identifiesItselfByShirtNumber() {
        Athlete athlete = athleteWearing(10);

        assertTrue(athlete.identify().contains("10"));
    }

    /**
     * The polymorphism this aggregate exists to show: one call, two answers, and no
     * `instanceof` at the call site deciding which is which.
     */
    @Test
    void answersTheSameCallDifferentlyFromAnOperator() {
        List<Person> people = List.of(athleteWearing(10), anOperator());

        List<String> identifications = people.stream().map(Person::identify).toList();

        assertNotEquals(identifications.get(0), identifications.get(1));
    }

    @Test
    void exposesShirtNumberAndPosition() {
        Athlete athlete = new Athlete("Rafael Lopes", "111.222.333-44",
                                      LocalDate.now().minusYears(22), 7, "winger");

        assertEquals(7, athlete.getShirtNumber());
        assertEquals("winger", athlete.getPosition());
    }

    @Test
    void rejectsAShirtNumberBelowOne() {
        assertThrows(IllegalArgumentException.class, () -> athleteWearing(0));
    }

    @Test
    void rejectsAShirtNumberAboveNinetyNine() {
        assertThrows(IllegalArgumentException.class, () -> athleteWearing(100));
    }

    @Test
    void acceptsTheShirtNumbersAtBothEndsOfTheRange() {
        assertEquals(1, athleteWearing(1).getShirtNumber());
        assertEquals(99, athleteWearing(99).getShirtNumber());
    }

    @Test
    void isEqualToAnotherAthleteHoldingTheSameDocument() {
        Athlete registered = new Athlete("Rafael Lopes", "111.222.333-44",
                                         LocalDate.now().minusYears(22), 7, "winger");
        Athlete sameDocument = new Athlete("R. Lopes", "111.222.333-44",
                                           LocalDate.now().minusYears(22), 9, "striker");

        assertEquals(registered, sameDocument);
        assertEquals(registered.hashCode(), sameDocument.hashCode());
    }

    @Test
    void isEqualToAnOperatorHoldingTheSameDocument() {
        Athlete athlete = new Athlete("Artur Kraemer", "555.666.777-88",
                                      LocalDate.now().minusYears(30), 5, "defender");
        Operator operator = new Operator("Artur Kraemer", "555.666.777-88",
                                         LocalDate.now().minusYears(30), "OP-1", "evening");

        assertEquals(athlete, operator);
    }

    private Athlete athleteWearing(int shirtNumber) {
        return new Athlete("Rafael Lopes", "111.222.333-44",
                           LocalDate.now().minusYears(22), shirtNumber, "winger");
    }

    private Operator anOperator() {
        return new Operator("Inaurrara Flores", "999.888.777-66",
                            LocalDate.now().minusYears(28), "OP-7", "evening");
    }
}
