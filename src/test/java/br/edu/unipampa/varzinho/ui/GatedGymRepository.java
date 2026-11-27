package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.port.GymRepository;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.domain.structure.Gym;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/** A gym store that counts saves, can be told to fail, and can hold a save at its gate. */
final class GatedGymRepository implements GymRepository {
    final SwingWork.Gate gate = new SwingWork.Gate();
    private final AtomicInteger saves = new AtomicInteger();
    private volatile boolean failing;

    void failFromNowOn() { failing = true; }

    int saves() { return saves.get(); }

    @Override
    public void save(Gym gym) throws RepositoryException {
        gate.pass();
        if (failing) throw new RepositoryException("disk unplugged");
        saves.incrementAndGet();
    }

    @Override
    public Optional<Gym> load() {
        return Optional.empty();
    }
}
