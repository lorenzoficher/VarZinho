package br.edu.unipampa.varzinho.infrastructure.persistence;

import br.edu.unipampa.varzinho.application.port.OperatorStore;
import br.edu.unipampa.varzinho.application.port.StorableOperators;
import br.edu.unipampa.varzinho.domain.people.Operator;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Keeps operator accounts in memory only. */
public final class InMemoryOperatorStore implements OperatorStore {
    private final Map<String, Operator> operatorsByUsername = new LinkedHashMap<>();

    /** Starts with no account, as on a first run. */
    public InMemoryOperatorStore() { }

    @Override
    public void save(Operator operator) {
        StorableOperators.requireStorable(operator);
        if (operatorsByUsername.containsKey(operator.getUsername())) {
            throw new IllegalArgumentException("username " + operator.getUsername() + " is already taken");
        }
        operatorsByUsername.put(operator.getUsername(), operator);
    }

    @Override
    public void replace(Operator operator) {
        StorableOperators.requireStorable(operator);
        if (!operatorsByUsername.containsKey(operator.getUsername())) {
            throw new IllegalArgumentException("no operator has the username " + operator.getUsername());
        }
        operatorsByUsername.put(operator.getUsername(), operator);
    }

    @Override
    public Optional<Operator> findByUsername(String username) {
        if (username == null) return Optional.empty();
        return Optional.ofNullable(operatorsByUsername.get(username.toLowerCase(Locale.ROOT)));
    }

    @Override
    public boolean hasAny() {
        return !operatorsByUsername.isEmpty();
    }
}
