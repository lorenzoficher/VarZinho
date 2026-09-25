package br.edu.unipampa.varzinho;

import br.edu.unipampa.varzinho.exception.RepositoryException;
import br.edu.unipampa.varzinho.repository.CsvHighlightRepository;
import br.edu.unipampa.varzinho.repository.HighlightRepository;
import br.edu.unipampa.varzinho.ui.VarZinhoWindow;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.nio.file.Path;

public final class Main {
    private Main() { }

    public static void main(String[] args) {
        try {
            HighlightRepository repository = new CsvHighlightRepository(
                    Path.of("data", "highlights.csv"));
            SwingUtilities.invokeLater(() -> new VarZinhoWindow(repository).setVisible(true));
        } catch (RepositoryException exception) {
            JOptionPane.showMessageDialog(null,
                    "Could not open the archive: " + exception.getMessage(),
                    "VarZinho", JOptionPane.ERROR_MESSAGE);
        }
    }
}
