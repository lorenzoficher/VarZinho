/**
 * The contracts the use cases need from the outside world, implemented in
 * {@code infrastructure.persistence}.
 *
 * <p>{@link br.edu.unipampa.varzinho.application.port.HighlightRepository},
 * {@link br.edu.unipampa.varzinho.application.port.GymRepository} and
 * {@link br.edu.unipampa.varzinho.application.port.OperatorStore} each have a CSV and a
 * PostgreSQL adapter, and neither is the contract. Every storage failure surfaces as a
 * {@link br.edu.unipampa.varzinho.application.port.RepositoryException}.
 */
package br.edu.unipampa.varzinho.application.port;
