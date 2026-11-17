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

    public OperatorView getOperator() {
        return operator;
    }

    public OperatorView register(String name, String username, String password)
            throws RepositoryException {
        Operator registered = new Operator(name, username, password);
        store.save(registered);
        return OperatorView.from(registered);
    }
}
