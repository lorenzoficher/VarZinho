package br.edu.unipampa.varzinho.repository;

import br.edu.unipampa.varzinho.domain.highlight.Highlight;

/**
 * The one rule every {@link HighlightRepository} applies before it stores anything.
 *
 * <p>It lives here, not in each implementation, so that a highlight one repository accepts
 * is a highlight any other accepts too — even one whose storage could hold more.
 */
final class StorableHighlights {

    private StorableHighlights() {
    }

    /**
     * @throws IllegalArgumentException if the highlight is {@code null}, or its id, camera id
     *     or clip path holds a comma or a line break
     */
    static void requireStorable(Highlight highlight) {
        if (highlight == null) {
            throw new IllegalArgumentException("there is no highlight to save");
        }
        String[] texts = {highlight.getId(), highlight.getCameraId(), highlight.getClip().getFilePath()};
        for (String text : texts) {
            if (text.contains(",") || text.contains("\n") || text.contains("\r")) {
                throw new IllegalArgumentException("cannot store a value with a comma or line break: " + text);
            }
        }
    }
}
