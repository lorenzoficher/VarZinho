package br.edu.unipampa.varzinho.domain.capture;

import br.edu.unipampa.varzinho.domain.highlight.VideoClip;
import br.edu.unipampa.varzinho.enums.Resolution;

import java.util.List;

/**
 * A camera that can be panned, tilted and zoomed while it records.
 *
 * <p>Where it is pointing is part of what it captured, so the clip it produces is
 * named after its aim as well as its id — two clips taken at the same second from
 * the same camera looking elsewhere are not the same footage.
 */
public final class PtzCamera extends Camera {

    private static final int MIN_PAN = 0;
    private static final int MAX_PAN = 359;
    private static final int MIN_TILT = -90;
    private static final int MAX_TILT = 90;
    private static final int MIN_ZOOM = 1;
    private static final int MAX_ZOOM = 10;

    private int pan;
    private int tilt;
    private int zoom;

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

    public int getPan() {
        return pan;
    }

    public int getTilt() {
        return tilt;
    }

    public int getZoom() {
        return zoom;
    }
}
