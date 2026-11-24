package br.edu.unipampa.varzinho.domain.capture;

import br.edu.unipampa.varzinho.domain.highlight.VideoClip;
import br.edu.unipampa.varzinho.enums.Resolution;

import java.util.List;

/**
 * A camera that can be panned, tilted and zoomed.
 *
 * <p>Where it is pointing is part of what it captured, so the clip it produces is
 * named after its aim as well as its id — two clips taken at the same second from
 * the same camera looking elsewhere are not the same footage.
 */
public final class PtzCamera extends Camera implements Aimable {

    private static final int MIN_PAN = 0;
    private static final int MAX_PAN = 359;
    private static final int MIN_TILT = -90;
    private static final int MAX_TILT = 90;
    private static final int MIN_ZOOM = 1;
    private static final int MAX_ZOOM = 10;

    private int pan;
    private int tilt;
    private int zoom;

    /**
     * Builds a PTZ camera, switched off, at its rest position: pan 0, tilt 0, zoom 1.
     *
     * @param id the identifier, unique across the gym the camera is installed in
     * @param model the make and model of the equipment
     * @param resolution the resolution it records at
     * @param bufferSeconds how many of the most recent seconds the camera keeps
     * @throws IllegalArgumentException if the id or model is missing or blank, the
     *         resolution is missing, or the buffer is not at least one second long
     */
    public PtzCamera(String id, String model, Resolution resolution, int bufferSeconds) {
        super(id, model, resolution, bufferSeconds);
        this.zoom = 1;
    }

    /**
     * Aims the camera. The ranges are the ones a domo head physically reaches: a full
     * turn of pan, a quarter turn of tilt each way, and ten steps of zoom.
     *
     * @throws IllegalArgumentException if any of the three is outside its range
     */
    @Override
    public void moveTo(int pan, int tilt, int zoom) {
        require(pan, MIN_PAN, MAX_PAN, "pan");
        require(tilt, MIN_TILT, MAX_TILT, "tilt");
        require(zoom, MIN_ZOOM, MAX_ZOOM, "zoom");
        this.pan = pan;
        this.tilt = tilt;
        this.zoom = zoom;
    }

    private static void require(int value, int min, int max, String name) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                    name + " runs from " + min + " to " + max + ", not " + value);
        }
    }

    @Override
    public VideoClip captureLastSeconds(int seconds) {
        List<Frame> window = recordedWindow(seconds);
        return clipFrom(clipPath(window), seconds);
    }

    @Override
    public String describe() {
        return describeAs("PTZ") + " · pan " + pan + " tilt " + tilt + " zoom " + zoom;
    }

    private String clipPath(List<Frame> window) {
        Frame last = window.get(window.size() - 1);
        return clipName(last) + "-p" + pan + "t" + tilt + "z" + zoom + ".mp4";
    }

    /** {@return the horizontal aim, in degrees, from 0 to 359} */
    public int getPan() {
        return pan;
    }

    /** {@return the vertical aim, in degrees, from -90 to 90} */
    public int getTilt() {
        return tilt;
    }

    /** {@return the zoom step, from 1 to 10} */
    public int getZoom() {
        return zoom;
    }
}
