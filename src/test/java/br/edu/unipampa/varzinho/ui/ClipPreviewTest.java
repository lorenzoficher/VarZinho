package br.edu.unipampa.varzinho.ui;

import org.junit.jupiter.api.Test;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClipPreviewTest {
    @Test
    void clipPitchStaysCenteredAndFitsAtDifferentWindowSizes() {
        PitchPanel preview = PitchPanel.clipCover();
        for (Dimension size : List.of(new Dimension(580, 260), new Dimension(750, 301),
                new Dimension(340, 400))) {
            preview.setSize(size);
            Rectangle field = preview.fieldBounds();
            assertEquals(size.width / 2.0, field.getCenterX(), 1);
            assertEquals(size.height / 2.0, field.getCenterY(), 1);
            assertTrue(field.x > 0 && field.y > 0);
            assertEquals(2, (double) field.width / field.height, 0.02);
        }
    }
}
