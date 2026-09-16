package br.edu.unipampa.varzinho.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ResolutionTest {

    @Test
    void labelsAResolutionByItsNameAndDimensions() {
        assertEquals("Full HD (1920x1080)", Resolution.FULL_HD.label());
    }

    @Test
    void labelsTheSmallestResolutionTheSameWay() {
        assertEquals("HD (1280x720)", Resolution.HD.label());
    }

    @Test
    void keepsTheConstantNameSoItRoundTripsThroughValueOf() {
        assertEquals(Resolution.ULTRA_HD, Resolution.valueOf(Resolution.ULTRA_HD.name()));
    }
}
