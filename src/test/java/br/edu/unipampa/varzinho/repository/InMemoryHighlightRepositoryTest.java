package br.edu.unipampa.varzinho.repository;

class InMemoryHighlightRepositoryTest extends HighlightRepositoryContractTest {

    @Override
    protected HighlightRepository createRepository() {
        return new InMemoryHighlightRepository();
    }
}
