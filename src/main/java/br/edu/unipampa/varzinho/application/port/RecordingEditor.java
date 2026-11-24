package br.edu.unipampa.varzinho.application.port;

/** Changes a completed recording, including the locally owned video when it exists. */
public interface RecordingEditor {

    /**
     * Gives a completed recording a new name; its video stays where it is.
     *
     * @param id the recording to rename
     * @param title the new name, on one line
     * @throws IllegalArgumentException if the name is blank or spans more than one line
     * @throws RepositoryException if no recording has this id, or it cannot be stored
     */
    void rename(String id, String title) throws RepositoryException;

    /**
     * Removes a completed recording and, when the video file is one this system owns, the
     * file too. Nothing is lost halfway: if either part fails, both stay. An unknown id
     * changes nothing.
     *
     * @param id the recording to remove
     * @throws RepositoryException if the video is not one this system may delete, is shared
     *     with another recording, or the recording or its file cannot be removed
     */
    void delete(String id) throws RepositoryException;
}
