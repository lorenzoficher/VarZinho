package br.edu.unipampa.varzinho.domain.capture;

/**
 * Equipment that can be pointed somewhere else.
 *
 * <p>A capability, not a kind of camera: it lives apart from {@link Camera} because of
 * the Interface Segregation Principle — no client should be forced to depend on
 * methods it does not use. A camera bolted to the wall, or a phone propped on a stand,
 * is never made to answer an aim it cannot take. A caller asks
 * {@code camera instanceof Aimable} rather than which class it is holding, and a new
 * kind of camera that turns its head joins in by implementing this alone.
 *
 * <p>Aiming is independent of recording: the equipment takes a new aim whether or not
 * it is recording at the time.
 */
public interface Aimable {

    /**
     * Points the equipment at a new aim, or refuses it whole: a rejected move leaves
     * the previous aim untouched.
     *
     * @param pan the horizontal angle, in degrees
     * @param tilt the vertical angle, in degrees
     * @param zoom the zoom step
     * @throws IllegalArgumentException if any of the three is outside what the
     *         equipment physically reaches
     */
    void moveTo(int pan, int tilt, int zoom);
}
