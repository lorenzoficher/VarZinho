package br.edu.unipampa.varzinho.bootstrap;

import java.time.Duration;
import java.util.Optional;

/**
 * Reads how long recordings are kept from {@value #VARIABLE}: seven days when it is unset,
 * the number of days it gives, or no expiry at all when it is {@code 0}.
 */
final class RetentionSetting {
    static final String VARIABLE = "VARZINHO_RETENTION_DAYS";
    static final int DEFAULT_DAYS = 7;

    private RetentionSetting() {
    }

    /**
     * @param value the variable's value, {@code null} when it is unset
     * @return the retention period, or empty when expiry is turned off
     * @throws IllegalArgumentException if the value is not a whole number of days, zero or more
     */
    static Optional<Duration> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.of(Duration.ofDays(DEFAULT_DAYS));
        }
        int days;
        try {
            days = Integer.parseInt(value.strip());
        } catch (NumberFormatException notANumber) {
            throw new IllegalArgumentException(VARIABLE + " must be a whole number of days, not '"
                    + value.strip() + "'", notANumber);
        }
        if (days < 0) {
            throw new IllegalArgumentException(VARIABLE + " cannot be negative, not " + days);
        }
        return days == 0 ? Optional.empty() : Optional.of(Duration.ofDays(days));
    }
}
