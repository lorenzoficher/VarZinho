package br.edu.unipampa.varzinho.domain.people;

import java.time.LocalDate;

/**
 * Someone who plays at the gym, and who presses the button when a play is worth
 * keeping.
 *
 * <p>Pressing it records nothing about them: the trigger carries no identity, so an
 * athlete asking for a clip later goes to the operator, who searches by court and
 * time.
 */
public final class Athlete extends Person {

    private static final int MIN_SHIRT_NUMBER = 1;
    private static final int MAX_SHIRT_NUMBER = 99;

    private final int shirtNumber;
    private final String position;

    public Athlete(String name, String document, LocalDate birthDate,
                   int shirtNumber, String position) {
        super(name, document, birthDate);
        if (shirtNumber < MIN_SHIRT_NUMBER || shirtNumber > MAX_SHIRT_NUMBER) {
            throw new IllegalArgumentException(
                    "a shirt number runs from " + MIN_SHIRT_NUMBER + " to " + MAX_SHIRT_NUMBER
                    + ", not " + shirtNumber);
        }
        this.shirtNumber = shirtNumber;
        this.position = position;
    }

    @Override
    public String identify() {
        return getName() + " #" + shirtNumber;
    }

    public int getShirtNumber() {
        return shirtNumber;
    }

    public String getPosition() {
        return position;
    }
}
