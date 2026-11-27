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

    /**
     * Gives access to the operators kept in a store.
     *
     * @param store where operator accounts are kept
     * @throws IllegalArgumentException if the store is missing
     */
    public OperatorAccess(OperatorStore store) {
        if (store == null) throw new IllegalArgumentException("operator access needs an operator store");
        this.store = store;
    }

    /**
     * Tells the window whether to ask for the first operator instead of a sign-in.
     *
     * @return whether no operator account exists yet
     * @throws RepositoryException if the operators cannot be read
     */
    public boolean needsFirstOperator() throws RepositoryException {
        return !store.hasAny();
    }

    /**
     * Creates the first operator account and signs it in. Only possible while there is none.
     *
     * @param name the operator's name
     * @param username the name to sign in with
     * @param password the password, following the operator's password rules
     * @return the new operator, signed in
     * @throws IllegalStateException if an operator account already exists
     * @throws IllegalArgumentException if the name, username or password is invalid
     * @throws RepositoryException if the operators cannot be read or written
     */
    public SignedInOperator createFirst(String name, String username, String password)
            throws RepositoryException {
        if (store.hasAny()) {
            throw new IllegalStateException("the first operator has already been created");
        }
        Operator operator = new Operator(name, username, password);
        store.save(operator);
        return new SignedInOperator(operator, store);
    }

    /**
     * Signs an operator in. An unknown username and a wrong password fail the same way, so
     * the message does not tell which usernames exist.
     *
     * @param username the username, in any case
     * @param password the password
     * @return the signed-in operator
     * @throws IllegalArgumentException if the username or password is incorrect
     * @throws RepositoryException if the operators cannot be read
     */
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
     * @param username the operator asking
     * @param email where the link would go; checked for shape, then forgotten
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
     * @param username the operator whose password changes
     * @param newPassword the new password
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
