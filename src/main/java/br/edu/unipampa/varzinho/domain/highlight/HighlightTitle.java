package br.edu.unipampa.varzinho.domain.highlight;

/**
 * The name someone gives a highlight: one readable line, never blank.
 *
 * <p>The one place the naming rule lives. A {@link Highlight} takes its name through
 * this type, and so does any storage that renames a highlight without loading it,
 * so the rule cannot drift between the entity and an adapter.
 */
public final class HighlightTitle {

    private final String text;

    /**
     * Checks a name as someone typed it.
     *
     * @param text the name, with or without spaces around it
     * @throws IllegalArgumentException if the name is missing, blank, or contains a
     *         line break or another control character
     */
    public HighlightTitle(String text) {
        if (text == null || text.isBlank() || text.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("a recording needs a name on one line");
        }
        this.text = text.strip();
    }

    /** {@return the name without leading or trailing spaces} */
    public String text() { return text; }

    @Override
    public boolean equals(Object other) {
        return other instanceof HighlightTitle && text.equals(((HighlightTitle) other).text);
    }

    @Override
    public int hashCode() { return text.hashCode(); }

    @Override
    public String toString() { return text; }
}
