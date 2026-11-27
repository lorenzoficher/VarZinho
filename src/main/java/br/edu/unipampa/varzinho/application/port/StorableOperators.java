package br.edu.unipampa.varzinho.application.port;

import br.edu.unipampa.varzinho.domain.people.Operator;

/** The representation rule shared by every {@link OperatorStore}. */
public final class StorableOperators {
    private StorableOperators() { }

    /**
     * Refuses an operator that some store could not represent, so every adapter accepts
     * the same accounts.
     *
     * @param operator the account about to be stored
     * @throws IllegalArgumentException if the operator is {@code null}, or its name, username
     *     or password hash holds a comma or a line break
     */
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
