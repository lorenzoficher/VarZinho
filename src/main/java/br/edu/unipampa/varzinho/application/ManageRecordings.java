package br.edu.unipampa.varzinho.application;

import br.edu.unipampa.varzinho.application.port.RecordingEditor;
import br.edu.unipampa.varzinho.application.port.RepositoryException;

/**
 * Renames and removes completed recordings in the archive, so the window changes the
 * archive through a use case instead of reaching the storage port itself.
 *
 * <p>It adds no rule of its own: what makes a name valid, and whether a video file goes
 * with the recording, is the {@link RecordingEditor}'s contract.
 */
public final class ManageRecordings {

    private final RecordingEditor editor;

    /**
     * @param editor what changes the stored recordings and their video files
     * @throws IllegalArgumentException if the editor is missing
     */
    public ManageRecordings(RecordingEditor editor) {
        if (editor == null) throw new IllegalArgumentException("managing recordings needs a recording editor");
        this.editor = editor;
    }

    /**
     * Gives a completed recording a new name; its video stays where it is.
     *
     * @param id the recording to rename
     * @param title the new name, on one line
     * @throws IllegalArgumentException if the name is blank or spans more than one line
     * @throws RepositoryException if no recording has this id, or it cannot be stored
     */
    public void rename(String id, String title) throws RepositoryException {
        editor.rename(id, title);
    }

    /**
     * Removes a completed recording and, when this system owns its video file, the file
     * too. Nothing is lost halfway, and an unknown id changes nothing.
     *
     * @param id the recording to remove
     * @throws RepositoryException if the video may not be deleted, or the recording or its
     *     file cannot be removed
     */
    public void delete(String id) throws RepositoryException {
        editor.delete(id);
    }
}
