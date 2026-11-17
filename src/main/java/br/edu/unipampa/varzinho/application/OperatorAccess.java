package br.edu.unipampa.varzinho.application;

import br.edu.unipampa.varzinho.application.port.OperatorStore;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.domain.people.Operator;

/** Creates the first account and authenticates operators without exposing entities. */
public final class OperatorAccess {
    private static final String INVALID_CREDENTIALS = "username or password is incorrect";
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
}
