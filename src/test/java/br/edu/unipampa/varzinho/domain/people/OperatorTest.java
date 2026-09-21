package br.edu.unipampa.varzinho.domain.people;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class OperatorTest {

    @Test
    void identifiesItselfByBadge() {
        Operator operator = operatorBadged("OP-7");

        assertTrue(operator.identify().contains("OP-7"));
    }

    @Test
    void exposesBadgeAndShift() {
        Operator operator = new Operator("Inaurrara Flores", "999.888.777-66",
                                         LocalDate.now().minusYears(28), "OP-7", "evening");

        assertEquals("OP-7", operator.getBadge());
        assertEquals("evening", operator.getShift());
    }

    @Test
    void rejectsABlankBadge() {
        assertThrows(IllegalArgumentException.class, () -> operatorBadged("   "));
    }

    @Test
    void rejectsAMissingBadge() {
        assertThrows(IllegalArgumentException.class, () -> operatorBadged(null));
    }

    private Operator operatorBadged(String badge) {
        return new Operator("Inaurrara Flores", "999.888.777-66",
                            LocalDate.now().minusYears(28), badge, "evening");
    }
}
