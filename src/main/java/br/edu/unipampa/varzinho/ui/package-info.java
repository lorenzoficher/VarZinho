/**
 * The Swing window: every screen the operator sees, and no business rule.
 *
 * <p>It reaches the model only through the use cases in {@code application}, never the domain or
 * the adapters directly, and shows the read models those use cases return.
 * {@link br.edu.unipampa.varzinho.ui.VarZinhoWindow} is the sidebar workspace, in light and dark
 * themes. Identifiers and log messages are in English; the text the operator reads is Portuguese,
 * translated at this boundary by {@link br.edu.unipampa.varzinho.ui.PortugueseText}.
 */
package br.edu.unipampa.varzinho.ui;
