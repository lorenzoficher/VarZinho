package br.edu.unipampa.varzinho.domain.people;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class PersonTest {

    @Test
    void keepsTheNameAndDocumentItWasBuiltWith() {
        Person person = personCalled("Lara Rios", "012.345.678-90");

        assertEquals("Lara Rios", person.getName());
        assertEquals("012.345.678-90", person.getDocument());
    }

    @Test
    void countsFullYearsElapsedSinceTheBirthDate() {
        Person person = personBornOn(LocalDate.now().minusYears(20).minusMonths(5));

        assertEquals(20, person.age());
    }

    @Test
    void isStillAYearYoungerOnTheDayBeforeTheBirthday() {
        Person person = personBornOn(LocalDate.now().minusYears(20).plusDays(1));

        assertEquals(19, person.age());
    }

    @Test
    void turnsAYearOlderOnTheBirthdayItself() {
        Person person = personBornOn(LocalDate.now().minusYears(20));

        assertEquals(20, person.age());
    }

    @Test
    void rejectsABlankName() {
        assertThrows(IllegalArgumentException.class,
                     () -> personCalled("   ", "012.345.678-90"));
    }

    @Test
    void rejectsABlankDocument() {
        assertThrows(IllegalArgumentException.class,
                     () -> personCalled("Lara Rios", "   "));
    }

    @Test
    void rejectsABirthDateInTheFuture() {
        assertThrows(IllegalArgumentException.class,
                     () -> personBornOn(LocalDate.now().plusDays(1)));
    }

    @Test
    void rejectsAMissingBirthDate() {
        assertThrows(IllegalArgumentException.class,
                     () -> personBornOn(null));
    }

    private Person personBornOn(LocalDate birthDate) {
        return person("Lara Rios", "012.345.678-90", birthDate);
    }

    private Person personCalled(String name, String document) {
        return person(name, document, LocalDate.now().minusYears(20));
    }

    /**
     * `Person` is abstract, so exercising it needs a concrete subtype. This one stays
     * inside the test rather than borrowing `Athlete`: the shared behaviour has to be
     * provable without depending on either subclass.
     */
    private Person person(String name, String document, LocalDate birthDate) {
        return new Person(name, document, birthDate) {
            @Override
            public String identify() {
                return getName();
            }
        };
    }
}
