package br.edu.unipampa.varzinho.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class RetentionSettingTest {

    @Test
    void keepsRecordingsForSevenDaysWhenNothingIsSet() {
        assertEquals(Optional.of(Duration.ofDays(7)), RetentionSetting.parse(null));
        assertEquals(Optional.of(Duration.ofDays(7)), RetentionSetting.parse("  "));
    }

    @Test
    void keepsRecordingsForTheNumberOfDaysGiven() {
        assertEquals(Optional.of(Duration.ofDays(30)), RetentionSetting.parse(" 30 "));
    }

    @Test
    void turnsExpiryOffWithZero() {
        assertEquals(Optional.empty(), RetentionSetting.parse("0"));
    }

    @Test
    void refusesAValueThatIsNotAWholeNumberOfDays() {
        assertThrows(IllegalArgumentException.class, () -> RetentionSetting.parse("7d"));
        assertThrows(IllegalArgumentException.class, () -> RetentionSetting.parse("-3"));
        assertThrows(IllegalArgumentException.class, () -> RetentionSetting.parse("1.5"));
        assertThrows(IllegalArgumentException.class, () -> RetentionSetting.parse("99999999999"));
    }
}
