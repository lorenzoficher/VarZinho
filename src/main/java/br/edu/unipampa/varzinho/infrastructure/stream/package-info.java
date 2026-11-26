/**
 * The FFmpeg adapter that records a phone and writes its clips as real {@code .mp4} files.
 *
 * <p>FFmpeg keeps the phone's stream in a ring of one-second segment files; a capture joins the
 * segments of its seconds without re-encoding them. FFmpeg is an external program found on the
 * {@code PATH} at runtime, needed only when {@code VARZINHO_STREAM_URL} names a phone. Nothing
 * outside this package knows the phone is reached through FFmpeg.
 */
package br.edu.unipampa.varzinho.infrastructure.stream;
