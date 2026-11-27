package br.edu.unipampa.varzinho.domain.capture;

import br.edu.unipampa.varzinho.domain.highlight.VideoClip;
import br.edu.unipampa.varzinho.enums.Resolution;

import java.nio.file.Path;
import java.util.List;

/**
 * A camera that is not mounted on the court but streams to it over the network — a
 * phone on a tripod.
 *
 * <p>The only kind whose clip is a real file: it hands its window to an assembler that
 * writes the video, where the other kinds only name a file that nothing writes.
 */
public final class StreamCamera extends Camera {

    private final String streamUrl;
    private final ClipAssembler assembler;

    /**
     * Builds a stream camera, switched off. Building it opens no connection: the
     * address is only recorded here.
     *
     * @param id the identifier, unique across the gym the camera is installed in
     * @param model the make and model of the phone or device
     * @param resolution the resolution it streams at
     * @param bufferSeconds how many of the most recent seconds the camera keeps
     * @param streamUrl the address the device streams from
     * @param assembler what writes this camera's clips to disk
     * @throws IllegalArgumentException if the id, model or stream address is missing or
     *         blank, the resolution or assembler is missing, or the buffer is not at
     *         least one second long
     */
    public StreamCamera(String id, String model, Resolution resolution, int bufferSeconds,
            String streamUrl, ClipAssembler assembler) {
        super(id, model, resolution, bufferSeconds);
        if (streamUrl == null || streamUrl.isBlank()) {
            throw new IllegalArgumentException("a stream camera needs the address it streams from");
        }
        if (assembler == null) {
            throw new IllegalArgumentException("a stream camera needs something to write its clips");
        }
        this.streamUrl = streamUrl;
        this.assembler = assembler;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The file is written before this returns: the camera waits for its assembler
     * however long the write takes, and never hands back a clip whose file is still to
     * come.
     *
     * @throws br.edu.unipampa.varzinho.exception.ClipAssemblyException if the file could
     *         not be written
     */
    @Override
    public VideoClip captureLastSeconds(int seconds) {
        List<Frame> window = recordedWindow(seconds);
        String path = clipName(window.get(window.size() - 1)) + ".mp4";
        assembler.assemble(window, Path.of(path));
        return clipFrom(path, seconds);
    }

    @Override
    public String describe() {
        return describeAs("stream");
    }

    /** {@return the address the device streams from, never blank} */
    public String getStreamUrl() {
        return streamUrl;
    }
}
