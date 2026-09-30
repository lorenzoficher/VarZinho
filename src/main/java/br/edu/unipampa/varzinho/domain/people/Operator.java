package br.edu.unipampa.varzinho.domain.people;

import java.time.LocalDate;

/**
 * Gym staff, and the person who holds the archive. When an athlete asks for a clip,
 * the operator is who finds it.
 *
 * <p>That search happens above the model: `domain/` must not know `repository/`, so an
 * operator holds no archive of their own and has no method for listing one.
 */
public final class Operator extends Person {

    private final String badge;
    private final String shift;

    public Operator(String name, String document, LocalDate birthDate,
                    String badge, String shift) {
        super(name, document, birthDate);
        if (badge == null || badge.isBlank()) {
            throw new IllegalArgumentException("an operator needs a badge");
        }
        this.badge = badge;
        this.shift = shift;
    }

    @Override
    public String identify() {
        return getName() + " (badge " + badge + ")";
    }

    public String getBadge() {
        return badge;
    }

    public String getShift() {
        return shift;
    }
}
