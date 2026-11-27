package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.port.OperatorStore;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.domain.people.Operator;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryOperatorStore;

import java.util.Optional;

/** An operator store in memory whose saves and lookups can be held at a gate. */
final class GatedOperatorStore implements OperatorStore {
    final SwingWork.Gate gate = new SwingWork.Gate();
    private final InMemoryOperatorStore stored = new InMemoryOperatorStore();

    @Override
    public void save(Operator operator) throws RepositoryException {
        gate.pass();
        stored.save(operator);
    }

    @Override
    public void replace(Operator operator) throws RepositoryException {
        gate.pass();
        stored.replace(operator);
    }

    @Override
    public Optional<Operator> findByUsername(String username) throws RepositoryException {
        gate.pass();
        return stored.findByUsername(username);
    }

    @Override
    public boolean hasAny() throws RepositoryException {
        return stored.hasAny();
    }
}
