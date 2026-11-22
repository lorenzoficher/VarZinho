package br.edu.unipampa.varzinho.application.port;

/** Changes a completed recording, including the locally owned video when it exists. */
public interface RecordingEditor {
    void rename(String id, String title) throws RepositoryException;
    void delete(String id) throws RepositoryException;
}
