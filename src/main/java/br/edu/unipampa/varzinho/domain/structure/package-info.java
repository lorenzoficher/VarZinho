/**
 * The physical layout of a gym: the {@link br.edu.unipampa.varzinho.domain.structure.Gym} and
 * the {@link br.edu.unipampa.varzinho.domain.structure.Court}s it owns, each with the cameras
 * installed on it.
 *
 * <p>A court is where a capture is triggered: pressing its button makes the first camera that is
 * recording freeze its last seconds into a clip. The collections here grow only through named behaviour
 * ({@code addCourt}, {@code installCamera}, {@code moveCamera}), never through a setter.
 * Part of the innermost layer: nothing here imports from the application, the adapters or the
 * window.
 */
package br.edu.unipampa.varzinho.domain.structure;
