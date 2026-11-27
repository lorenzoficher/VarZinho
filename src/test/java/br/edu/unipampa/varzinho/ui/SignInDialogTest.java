package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.OperatorAccess;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.domain.people.Operator;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryOperatorStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class SignInDialogTest {
    private final InMemoryOperatorStore store = new InMemoryOperatorStore();
    private final OperatorAccess access = new OperatorAccess(store);

    @Test
    void offersTheFirstOperatorFormWhenNoOperatorIsStored() throws RepositoryException {
        assertInstanceOf(FirstOperatorPanel.class, SignInDialog.formFor(access, message -> { }, operator -> { }));
    }

    @Test
    void offersTheSignInFormOnceAnOperatorIsStored() throws RepositoryException {
        store.save(new Operator("Ana Lima", "ana", "Str0ng!pass"));

        assertInstanceOf(SignInPanel.class, SignInDialog.formFor(access, message -> { }, operator -> { }));
    }
}
