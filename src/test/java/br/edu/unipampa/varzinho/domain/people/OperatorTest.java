package br.edu.unipampa.varzinho.domain.people;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OperatorTest {
    private static final String STRONG_PASSWORD = "Strong1!";

    @Test
    void rejectsEachBrokenPasswordRuleWithItsReason() {
        assertPasswordRefusal("Short1!", "8 characters");
        assertPasswordRefusal("lowercase1!", "uppercase");
        assertPasswordRefusal("UPPERCASE1!", "lowercase");
        assertPasswordRefusal("NoDigits!", "digit");
        assertPasswordRefusal("NoSymbol1", "symbol");
    }

    @Test
    void equalPasswordsReceiveDifferentSaltedHashes() {
        Operator first = operator("first");
        Operator second = operator("second");

        assertNotEquals(first.getPasswordHash(), second.getPasswordHash());
        assertFalse(first.getPasswordHash().contains(STRONG_PASSWORD));
    }

    @Test
    void checksTheRightPasswordAndRefusesAWrongOne() {
        Operator operator = operator("inaurrara");

        assertTrue(operator.checksPassword(STRONG_PASSWORD));
        assertFalse(operator.checksPassword("Wrong1!x"));
        assertFalse(operator.checksPassword(null));
    }

    @Test
    void aRestoredOperatorStillChecksItsPassword() {
        Operator original = operator("inaurrara");

        Operator restored = Operator.restore(original.getName(), original.getUsername(), original.getPasswordHash());

        assertTrue(restored.checksPassword(STRONG_PASSWORD));
        assertEquals(original.getPasswordHash(), restored.getPasswordHash());
    }

    @Test
    void usernamesAreEqualRegardlessOfCase() {
        Operator lowerCase = operator("inaurrara");
        Operator upperCase = operator("INAURRARA");

        assertTrue(lowerCase.equals(upperCase));
        assertEquals(lowerCase.hashCode(), upperCase.hashCode());
        assertEquals("inaurrara", upperCase.getUsername());
    }

    @Test
    void rejectsABlankOrWhitespaceUsername() {
        assertThrows(IllegalArgumentException.class,
                () -> new Operator("Inaurrara Flores", "  ", STRONG_PASSWORD));
        IllegalArgumentException whitespace = assertThrows(IllegalArgumentException.class,
                () -> new Operator("Inaurrara Flores", "inaur rara", STRONG_PASSWORD));

        assertTrue(whitespace.getMessage().contains("whitespace"));
    }

    @Test
    void refusesAnInvalidStoredHash() {
        assertThrows(IllegalArgumentException.class,
                () -> Operator.restore("Inaurrara Flores", "inaurrara", "not-a-pbkdf2-hash"));
    }

    private static Operator operator(String username) {
        return new Operator("Inaurrara Flores", username, STRONG_PASSWORD);
    }

    private static void assertPasswordRefusal(String password, String rule) {
        IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class,
                () -> new Operator("Inaurrara Flores", "inaurrara", password));
        assertTrue(refusal.getMessage().contains(rule), refusal.getMessage());
    }
}
