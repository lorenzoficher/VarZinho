package br.edu.unipampa.varzinho.application;

import br.edu.unipampa.varzinho.application.port.OperatorStore;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.domain.people.Operator;

/** The operations available only after an operator has signed in. */
public final class SignedInOperator {
    private final OperatorView operator;
    private final OperatorStore store;

    SignedInOperator(Operator operator, OperatorStore store) {
        this.operator = OperatorView.from(operator);
        this.store = store;
    }

    /** {@return the operator who signed in} */
    public OperatorView getOperator() {
        return operator;
    }

    /**
     * Registers another operator account. Only a signed-in operator can, which is why this
     * lives here and not in {@link OperatorAccess}.
     *
     * @param name the new operator's name
     * @param username the name they sign in with; must not be taken, in any case
     * @param password their password, following the operator's password rules
     * @return the new operator
     * @throws IllegalArgumentException if a value is invalid or the username is taken
     * @throws RepositoryException if the operators cannot be read or written
     */
    public OperatorView register(String name, String username, String password)
            throws RepositoryException {
        Operator registered = new Operator(name, username, password);
        store.save(registered);
        return OperatorView.from(registered);
    }
}
