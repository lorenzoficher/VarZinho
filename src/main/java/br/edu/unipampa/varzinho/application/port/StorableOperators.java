package br.edu.unipampa.varzinho.application.port;

import br.edu.unipampa.varzinho.domain.people.Operator;

/** The representation rule shared by every {@link OperatorStore}. */
public final class StorableOperators {
    private StorableOperators() { }

    public static void requireStorable(Operator operator) {
        if (operator == null) {
            throw new IllegalArgumentException("there is no operator to save");
        }
        String[] fields = {operator.getName(), operator.getUsername(), operator.getPasswordHash()};
        for (String field : fields) {
            if (field.contains(",") || field.contains("\n") || field.contains("\r")) {
                throw new IllegalArgumentException("cannot store a value with a comma or line break: " + field);
            }
        }
    }
}
