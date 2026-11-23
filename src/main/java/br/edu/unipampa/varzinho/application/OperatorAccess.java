package br.edu.unipampa.varzinho.application;

import br.edu.unipampa.varzinho.application.port.OperatorStore;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.domain.people.Operator;

import java.util.regex.Pattern;

/**
 * Creates the first account, authenticates operators and resets a forgotten password,
 * without exposing entities.
 *
 * <p>The password reset is a simulation: the e-mail address is checked for shape and
 * then forgotten, and nothing is sent. VarZinho runs on one machine with no mail server.
 */
public final class OperatorAccess {
    private static final String INVALID_CREDENTIALS = "username or password is incorrect";
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    private final OperatorStore store;

    public OperatorAccess(OperatorStore store) {
        if (store == null) throw new IllegalArgumentException("operator access needs an operator store");
        this.store = store;
    }

    public boolean needsFirstOperator() throws RepositoryException {
        return !store.hasAny();
    }

    public SignedInOperator createFirst(String name, String username, String password)
            throws RepositoryException {
        if (store.hasAny()) {
            throw new IllegalStateException("the first operator has already been created");
        }
        Operator operator = new Operator(name, username, password);
        store.save(operator);
        return new SignedInOperator(operator, store);
    }

    public SignedInOperator signIn(String username, String password) throws RepositoryException {
        Operator operator = store.findByUsername(username)
                .filter(found -> found.checksPassword(password))
                .orElseThrow(() -> new IllegalArgumentException(INVALID_CREDENTIALS));
        return new SignedInOperator(operator, store);
    }

    /**
     * Accepts a request for a password reset link. No link is sent: the caller moves
     * straight on to {@link #resetPassword}.
     *
     * @throws IllegalArgumentException if the username is blank or unknown, or the e-mail
     *     address is not well formed
     * @throws RepositoryException if the operators cannot be read
     */
    public void requestPasswordReset(String username, String email) throws RepositoryException {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("an operator needs a username");
        }
        if (email == null || !EMAIL.matcher(email.strip()).matches()) {
            throw new IllegalArgumentException("the e-mail address is not valid");
        }
        existing(username);
    }

    /**
     * Gives the operator a new password, keeping the name and the username.
     *
     * @throws IllegalArgumentException if the username is unknown or the password breaks
     *     the operator's password rules; the old password then still works
     * @throws RepositoryException if the operators cannot be read or written
     */
    public void resetPassword(String username, String newPassword) throws RepositoryException {
        Operator current = existing(username);
        store.replace(new Operator(current.getName(), current.getUsername(), newPassword));
    }

    private Operator existing(String username) throws RepositoryException {
        return store.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("no operator has the username " + username));
    }
}
