/**
 * The composition root: the one place that names every concrete adapter.
 *
 * <p>{@link br.edu.unipampa.varzinho.bootstrap.Launcher} puts the application together and hands
 * it to the window; {@link br.edu.unipampa.varzinho.bootstrap.Storage} picks CSV or PostgreSQL
 * once for all three ports; {@link br.edu.unipampa.varzinho.bootstrap.SampleGym} is the gym a
 * first run opens on. A new storage or a new way to reach a camera changes this package, never
 * the window or the model.
 */
package br.edu.unipampa.varzinho.bootstrap;
