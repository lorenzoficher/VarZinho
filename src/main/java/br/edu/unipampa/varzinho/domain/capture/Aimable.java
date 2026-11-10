package br.edu.unipampa.varzinho.domain.capture;

/**
 * Equipment that can be pointed somewhere else while it records.
 *
 * <p>A capability, not a kind of camera: it lives apart from {@link Camera} so that
 * a camera bolted to the wall, or a phone propped on a stand, is never made to answer
 * an aim it cannot take. A caller asks {@code camera instanceof Aimable} rather than
 * which class it is holding, and a new kind of camera that turns its head joins in by
 * implementing this alone.
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
