package br.edu.unipampa.varzinho.domain.highlight;

import br.edu.unipampa.varzinho.enums.Resolution;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HighlightTest {
    private static final Instant CAPTURED_AT = Instant.parse("2026-09-24T12:00:00Z");
    private static final VideoClip CLIP = new VideoClip("clips/h-1.mp4", 5, Resolution.FULL_HD, 10.5);

    @Test
    void rejectsInvalidCaptureMetadata() {
        assertThrows(IllegalArgumentException.class,
                () -> new Highlight(" ", CAPTURED_AT, 1, "camera-1", CLIP));
        assertThrows(IllegalArgumentException.class,
                () -> new Highlight("h-1", null, 1, "camera-1", CLIP));
        assertThrows(IllegalArgumentException.class,
                () -> new Highlight("h-1", CAPTURED_AT, 0, "camera-1", CLIP));
        assertThrows(IllegalArgumentException.class,
                () -> new Highlight("h-1", CAPTURED_AT, 1, " ", CLIP));
        assertThrows(IllegalArgumentException.class,
                () -> new Highlight("h-1", CAPTURED_AT, 1, "camera-1", null));
    }

    @Test
    void exposesItsCaptureMetadata() {
        Highlight highlight = new Highlight("h-1", CAPTURED_AT, 2, "camera-1", CLIP);

        assertEquals("h-1", highlight.getId());
        assertEquals(CAPTURED_AT, highlight.getCapturedAt());
        assertEquals(2, highlight.getCourtNumber());
        assertEquals("camera-1", highlight.getCameraId());
        assertEquals(CLIP, highlight.getClip());
    }

    @Test
    void describesTheCaptureWithoutNamingAPerson() {
        Highlight highlight = new Highlight("h-1", CAPTURED_AT, 2, "camera-1", CLIP);

        assertTrue(highlight.describe().contains("court 2"));
        assertTrue(highlight.describe().contains("camera camera-1"));
        assertTrue(highlight.describe().contains(CAPTURED_AT.toString()));
        assertTrue(highlight.describe().contains("5s"));
        assertFalse(highlight.describe().toLowerCase().contains("person"));
    }

    @Test
    void keepsDistinctIdentifiersAssignedToDifferentHighlights() {
        Highlight first = new Highlight("h-1", CAPTURED_AT, 1, "camera-1", CLIP);
        Highlight second = new Highlight("h-2", CAPTURED_AT, 1, "camera-1", CLIP);

        assertNotEquals(first.getId(), second.getId());
    }

    @Test
    void remainsImmutableAfterConstruction() {
        assertTrue(Arrays.stream(Highlight.class.getDeclaredFields())
                .allMatch(field -> Modifier.isPrivate(field.getModifiers())
                        && Modifier.isFinal(field.getModifiers())));
        assertTrue(Arrays.stream(Highlight.class.getDeclaredMethods())
                .map(method -> method.getName())
                .noneMatch(name -> name.startsWith("set")));
    }
}
