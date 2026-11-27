package br.edu.unipampa.varzinho.infrastructure.persistence;

import br.edu.unipampa.varzinho.application.port.GymRepository;

class InMemoryGymRepositoryTest extends GymRepositoryContractTest {

    @Override
    protected GymRepository createRepository() {
        return new InMemoryGymRepository();
    }
}
