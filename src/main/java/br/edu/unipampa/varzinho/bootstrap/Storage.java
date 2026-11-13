package br.edu.unipampa.varzinho.bootstrap;

import br.edu.unipampa.varzinho.application.port.GymRepository;
import br.edu.unipampa.varzinho.application.port.HighlightRepository;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.infrastructure.persistence.CsvGymRepository;
import br.edu.unipampa.varzinho.infrastructure.persistence.CsvHighlightRepository;
import br.edu.unipampa.varzinho.infrastructure.persistence.PostgresGymRepository;
import br.edu.unipampa.varzinho.infrastructure.persistence.PostgresHighlightRepository;

import java.nio.file.Path;
import java.util.function.UnaryOperator;

/**
 * Where this run keeps the gym and the highlight archive: the CSV files by default, or
 * PostgreSQL when {@value #URL_VARIABLE} names a database.
 *
 * <p>A database that is named but cannot be reached is never swapped for the files:
 * highlights quietly written to a CSV would look lost from the database.
 */
final class Storage {
    static final String URL_VARIABLE = "VARZINHO_DATABASE_URL";
    static final String USER_VARIABLE = "VARZINHO_DATABASE_USER";
    static final String PASSWORD_VARIABLE = "VARZINHO_DATABASE_PASSWORD";

    private static final String PASSWORD_PARAMETER = "(?i)([?&;]password=)[^&;]*";

    private final HighlightRepository highlights;
    private final GymRepository gym;

    private Storage(HighlightRepository highlights, GymRepository gym) {
        this.highlights = highlights;
        this.gym = gym;
    }

    /**
     * @param environment looks up a variable by name, answering {@code null} when it is unset
     * @param dataFolder where the CSV files live when no database is named
     * @throws RepositoryException if a database is named but has no user or cannot be
     *         reached; the message names the database, never its password
     */
    static Storage fromEnvironment(UnaryOperator<String> environment, Path dataFolder)
            throws RepositoryException {
        String url = environment.apply(URL_VARIABLE);
        if (url == null || url.isBlank()) {
            return new Storage(new CsvHighlightRepository(dataFolder.resolve("highlights.csv")),
                    new CsvGymRepository(dataFolder.resolve("gym.csv")));
        }
        return inDatabase(url, environment.apply(USER_VARIABLE), environment.apply(PASSWORD_VARIABLE));
    }

    private static Storage inDatabase(String url, String user, String password) throws RepositoryException {
        String shownUrl = url.replaceAll(PASSWORD_PARAMETER, "$1***");
        if (user == null || user.isBlank()) {
            throw new RepositoryException(URL_VARIABLE + " names " + shownUrl + " but "
                    + USER_VARIABLE + " is not set");
        }
        try {
            return new Storage(PostgresHighlightRepository.open(url, user, password),
                    PostgresGymRepository.open(url, user, password));
        } catch (RepositoryException failure) {
            // The adapter's own message repeats the raw url, which may carry the password.
            throw new RepositoryException("could not reach the database at " + shownUrl
                    + " (" + rootReason(failure) + ")", failure);
        }
    }

    private static String rootReason(Throwable failure) {
        Throwable root = failure;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return String.valueOf(root.getMessage()).replaceAll(PASSWORD_PARAMETER, "$1***");
    }

    HighlightRepository highlights() {
        return highlights;
    }

    GymRepository gym() {
        return gym;
    }
}
