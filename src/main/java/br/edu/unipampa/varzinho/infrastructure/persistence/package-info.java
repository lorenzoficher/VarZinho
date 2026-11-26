/**
 * The storage adapters: one CSV, one PostgreSQL and one in-memory implementation of each port
 * in {@code application.port}.
 *
 * <p>The CSV files live under {@code data/}; PostgreSQL is reached through plain JDBC, with no
 * object-relational mapper, and creates its tables on first use. Which one a run uses is
 * decided once, in {@code bootstrap.Storage}. These classes know the domain; the domain does
 * not know them.
 */
package br.edu.unipampa.varzinho.infrastructure.persistence;
