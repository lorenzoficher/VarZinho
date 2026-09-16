package br.edu.unipampa.varzinho.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RepositoryExceptionTest {

    @Test
    void staysCheckedSoTheCallerHasToDecideWhatToDoWithADamagedArchive() {
        assertFalse(RuntimeException.class.isAssignableFrom(RepositoryException.class));
    }

    @Test
    void readsACorruptedRowAsARepositoryFailure() {
        assertTrue(RepositoryException.class.isAssignableFrom(CorruptedRecordException.class));
    }

    @Test
    void keepsTheCauseThatExplainsTheCorruptedRow() {
        NumberFormatException cause = new NumberFormatException("for input string: \"thirty\"");

        CorruptedRecordException thrown = new CorruptedRecordException("row 4", cause);

        assertSame(cause, thrown.getCause());
        assertEquals("row 4", thrown.getMessage());
    }
}
