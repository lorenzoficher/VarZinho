package br.edu.unipampa.varzinho.infrastructure.persistence;

import br.edu.unipampa.varzinho.application.port.HighlightRepository;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryHighlightRepository;

class InMemoryHighlightRepositoryTest extends HighlightRepositoryContractTest {

    @Override
    protected HighlightRepository createRepository() {
        return new InMemoryHighlightRepository();
    }
}
