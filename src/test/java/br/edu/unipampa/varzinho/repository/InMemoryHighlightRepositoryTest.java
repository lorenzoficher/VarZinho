package br.edu.unipampa.varzinho.repository;

import br.edu.unipampa.varzinho.application.port.HighlightRepository;

class InMemoryHighlightRepositoryTest extends HighlightRepositoryContractTest {

    @Override
    protected HighlightRepository createRepository() {
        return new InMemoryHighlightRepository();
    }
}
