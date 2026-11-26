/**
 * The errors the domain raises about itself, all under
 * {@link br.edu.unipampa.varzinho.exception.DomainException}.
 *
 * <p>Each names a capture rule that was broken: a court with no active camera, a buffer with
 * nothing to freeze, a camera installed twice, a clip that could not be assembled. Invalid
 * arguments use {@code IllegalArgumentException} instead, and storage failures belong to the
 * application's ports, not here.
 */
package br.edu.unipampa.varzinho.exception;
