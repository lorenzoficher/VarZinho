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
        Path archive = Path.of("data", "highlights.csv");
        try {
            HighlightRepository repository = new CsvHighlightRepository(archive);
            if (args.length > 0 && "--console".equals(args[0])) {
                ConsoleDemo.run(repository);
            } else {
                SwingUtilities.invokeLater(() -> openWindow(repository));
            }
        } catch (RepositoryException exception) {
            showArchiveError(exception);
        }
    }

    private static void openWindow(HighlightRepository repository) {
        new VarZinhoWindow(repository).setVisible(true);
    }

    private static void showArchiveError(RepositoryException exception) {
        JOptionPane.showMessageDialog(null, "Could not open the archive: " + exception.getMessage(),
                "VarZinho", JOptionPane.ERROR_MESSAGE);
    }
}
