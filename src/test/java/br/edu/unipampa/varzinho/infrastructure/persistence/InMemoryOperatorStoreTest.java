package br.edu.unipampa.varzinho.infrastructure.persistence;

import br.edu.unipampa.varzinho.application.port.OperatorStore;

class InMemoryOperatorStoreTest extends OperatorStoreContractTest {
    @Override
    protected OperatorStore createStore() {
        return new InMemoryOperatorStore();
    }
}
