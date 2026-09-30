package br.edu.unipampa.varzinho.domain.people;

import java.time.LocalDate;
import java.time.Period;

/**
 * Someone the gym knows: an athlete who plays there, or an operator who works there.
 *
 * <p>Nobody here is ever recorded on a highlight. The button carries no identity, so
 * these classes exist because a gym has members and staff — not because a capture can
 * be attributed to anyone.
 *
 * <p>Identity is the document. Two people holding the same one are the same person
 * whatever they do at the gym, which is what lets the register refuse a document it
 * already holds.
 */
public abstract class Person {

    private final String name;
    private final String document;
    private final LocalDate birthDate;

    protected Person(String name, String document, LocalDate birthDate) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("a person needs a name");
        }
        if (document == null || document.isBlank()) {
            throw new IllegalArgumentException("a person needs a document");
        }
        if (birthDate == null) {
            throw new IllegalArgumentException("a person needs a birth date");
        }
        if (birthDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("a birth date cannot be in the future: " + birthDate);
        }
        this.name = name;
        this.document = document;
        this.birthDate = birthDate;
    }

    /**
     * @return full years elapsed since the birth date, counted the way a person counts
     *         them: the number turns on the birthday itself, never the day before
     */
    public int age() {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    /**
     * How this person is announced. What identifies somebody depends on what they do
     * at the gym, so each kind answers differently and no caller asks which is which.
     *
     * @return a line naming the person and the detail their role is known by
     */
    public abstract String identify();

    public String getName() {
        return name;
    }

    public String getDocument() {
        return document;
    }

    /**
     * Final, and keyed on the document alone. An athlete and an operator carrying the
     * same document are one person holding two roles, and the gym register depends on
     * that being true — comparing classes here would let the same document in twice.
     */
    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Person person)) {
            return false;
        }
        return document.equals(person.document);
    }

    @Override
    public final int hashCode() {
        return document.hashCode();
    }

    @Override
    public String toString() {
        return identify();
    }
}
