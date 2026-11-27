/**
 * The public spectator view: a web page that streams the phone's court live and sells a clip of
 * it through a simulated or real Pix payment.
 *
 * <p>{@link br.edu.unipampa.varzinho.infrastructure.spectator.SpectatorServer} listens on the
 * loopback only; reaching it from outside takes a reverse proxy or, in demo mode, a temporary
 * tunnel.
 */
package br.edu.unipampa.varzinho.infrastructure.spectator;
