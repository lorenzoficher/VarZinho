/**
 * The cameras and the memory that makes a highlight possible.
 *
 * <p>Every {@link br.edu.unipampa.varzinho.domain.capture.Camera} keeps a
 * {@link br.edu.unipampa.varzinho.domain.capture.CircularBuffer} of its most recent seconds,
 * overwriting the oldest as new {@link br.edu.unipampa.varzinho.domain.capture.Frame}s arrive.
 * The camera kinds are where the model's inheritance and polymorphism live: a
 * {@link br.edu.unipampa.varzinho.domain.capture.FixedCamera} only records, a
 * {@link br.edu.unipampa.varzinho.domain.capture.PtzCamera} can also be aimed
 * ({@link br.edu.unipampa.varzinho.domain.capture.Aimable}), and a
 * {@link br.edu.unipampa.varzinho.domain.capture.StreamCamera} turns a phone's footage into a
 * real video file through a {@link br.edu.unipampa.varzinho.domain.capture.ClipAssembler}.
 *
 * <p>No frame is decoded in Java. Fixed and PTZ cameras carry metadata only; the phone's video
 * work happens behind the {@code ClipAssembler} interface, implemented outside the domain.
 */
package br.edu.unipampa.varzinho.domain.capture;
