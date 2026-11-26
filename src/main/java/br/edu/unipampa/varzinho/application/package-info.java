/**
 * The use cases: what an operator can do, expressed against the domain and the ports, with no
 * knowledge of Swing, files, SQL or FFmpeg.
 *
 * <p>{@link br.edu.unipampa.varzinho.application.ManageGym} changes the gym's structure,
 * {@link br.edu.unipampa.varzinho.application.CaptureHighlight} turns a button press into a
 * stored highlight, {@link br.edu.unipampa.varzinho.application.BrowseArchive} and
 * {@link br.edu.unipampa.varzinho.application.ManageRecordings} read and edit the archive, and
 * {@link br.edu.unipampa.varzinho.application.OperatorAccess} signs operators in and registers
 * them. The read models ({@link br.edu.unipampa.varzinho.application.CourtView},
 * {@link br.edu.unipampa.varzinho.application.HighlightView}, ...) are what the window displays,
 * so the window never holds a domain object.
 */
package br.edu.unipampa.varzinho.application;
