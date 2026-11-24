package br.edu.unipampa.varzinho.application;

import br.edu.unipampa.varzinho.domain.people.Operator;

/** The immutable operator data an interface may display after sign-in. */
public final class OperatorView {
    private final String name;
    private final String username;

    private OperatorView(String name, String username) {
        this.name = name;
        this.username = username;
    }

    static OperatorView from(Operator operator) {
        return new OperatorView(operator.getName(), operator.getUsername());
    }

    /** {@return the operator's name} */
    public String getName() {
        return name;
    }

    /** {@return the name the operator signs in with} */
    public String getUsername() {
        return username;
    }
}
