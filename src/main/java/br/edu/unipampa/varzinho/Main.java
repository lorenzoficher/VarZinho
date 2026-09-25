package br.edu.unipampa.varzinho;

import br.edu.unipampa.varzinho.exception.RepositoryException;
import br.edu.unipampa.varzinho.repository.CsvHighlightRepository;

import java.nio.file.Path;

public final class Main {
    private Main() { }

    public static void main(String[] args) {
        try {
            ConsoleDemo.run(new CsvHighlightRepository(Path.of("data", "highlights.csv")));
        } catch (RepositoryException exception) {
            System.err.println("Could not open the archive: " + exception.getMessage());
        }
    }
}
