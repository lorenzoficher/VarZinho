package br.edu.unipampa.varzinho.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.edu.unipampa.varzinho.application.port.RecordingEditor;
import br.edu.unipampa.varzinho.application.port.RepositoryException;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ManageRecordingsTest {

    @Test
    void renamesTheRecordingThroughTheEditor() throws Exception {
        InMemoryRecordingEditor editor = new InMemoryRecordingEditor("first", "second");
        ManageRecordings recordings = new ManageRecordings(editor);

        recordings.rename("second", "Lance Fernando Costa");

        assertEquals("Lance Fernando Costa", editor.titleOf("second"));
        assertEquals("", editor.titleOf("first"));
    }

    @Test
    void deletesTheRecordingThroughTheEditor() throws Exception {
        InMemoryRecordingEditor editor = new InMemoryRecordingEditor("first", "second");
        ManageRecordings recordings = new ManageRecordings(editor);

        recordings.delete("first");

        assertFalse(editor.holds("first"));
        assertEquals("", editor.titleOf("second"));
    }

    @Test
    void rejectsAMultiLineTitleSoTheWindowCanAskAgain() {
        ManageRecordings recordings = new ManageRecordings(new InMemoryRecordingEditor("first"));

        assertThrows(IllegalArgumentException.class, () -> recordings.rename("first", "Lance\nnovo"));
    }

    @Test
    void reportsAStorageFailureWhenRenamingAnUnknownRecording() {
        ManageRecordings recordings = new ManageRecordings(new InMemoryRecordingEditor("first"));

        assertThrows(RepositoryException.class, () -> recordings.rename("missing", "Lance"));
    }

    @Test
    void reportsAStorageFailureWhenTheRecordingCannotBeDeleted() {
        InMemoryRecordingEditor editor = new InMemoryRecordingEditor("first");
        editor.refuseDeletion();
        ManageRecordings recordings = new ManageRecordings(editor);

        assertThrows(RepositoryException.class, () -> recordings.delete("first"));
    }

    @Test
    void cannotBeBuiltWithoutAnEditor() {
        assertThrows(IllegalArgumentException.class, () -> new ManageRecordings(null));
    }

    private static final class InMemoryRecordingEditor implements RecordingEditor {

        private final Map<String, String> titles = new HashMap<>();
        private boolean deletionRefused;

        InMemoryRecordingEditor(String... ids) {
            for (String id : ids) titles.put(id, "");
        }

        void refuseDeletion() { deletionRefused = true; }

        boolean holds(String id) { return titles.containsKey(id); }

        String titleOf(String id) { return titles.get(id); }

        @Override
        public void rename(String id, String title) throws RepositoryException {
            if (title == null || title.isBlank() || title.lines().count() > 1) {
                throw new IllegalArgumentException("a recording's name must be one non-blank line");
            }
            if (!titles.containsKey(id)) throw new RepositoryException("no recording " + id);
            titles.put(id, title);
        }

        @Override
        public void delete(String id) throws RepositoryException {
            if (deletionRefused) throw new RepositoryException("cannot delete recording " + id);
            titles.remove(id);
        }
    }
}
