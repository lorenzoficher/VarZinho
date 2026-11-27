package br.edu.unipampa.varzinho.ui;

/**
 * What a button does for the operator, which decides how it looks on every screen.
 *
 * <p>A panel names the role and {@link VarZinhoTheme#styleButton} draws it, so two buttons
 * that do the same kind of thing never end up looking different.
 */
enum ButtonRole {
    /** The capture button: the one action the whole window exists for. */
    PROMINENT,
    /** The action a screen or form is there to complete. */
    PRIMARY,
    /** Any other action, including those that open, copy or cancel. */
    SECONDARY,
    /** An action that removes something the operator cannot get back. */
    DESTRUCTIVE,
    /** A quiet action that reads as a link, such as "forgot my password". */
    LINK,
    /** A whole card that selects what it shows, such as a court on the dashboard. */
    TILE
}
