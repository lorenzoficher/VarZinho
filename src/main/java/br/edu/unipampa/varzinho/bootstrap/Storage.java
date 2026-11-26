package br.edu.unipampa.varzinho.bootstrap;

import br.edu.unipampa.varzinho.application.port.GymRepository;
import br.edu.unipampa.varzinho.application.port.HighlightRepository;
import br.edu.unipampa.varzinho.application.port.OperatorStore;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.infrastructure.persistence.CsvGymRepository;
import br.edu.unipampa.varzinho.infrastructure.persistence.CsvHighlightRepository;
import br.edu.unipampa.varzinho.infrastructure.persistence.CsvOperatorStore;
import br.edu.unipampa.varzinho.infrastructure.persistence.PostgresGymRepository;
import br.edu.unipampa.varzinho.infrastructure.persistence.PostgresHighlightRepository;
import br.edu.unipampa.varzinho.infrastructure.persistence.PostgresOperatorStore;

import java.nio.file.Path;
import java.util.Locale;
import java.util.function.UnaryOperator;

/**
 * Where this run keeps the gym, the highlight archive and the operator accounts: PostgreSQL by
 * default, or the CSV files under {@code data/} when {@value #MODE_VARIABLE} is {@code csv}.
 *
 * <p>With no variable set, the database is the one {@code docker-compose.yml} starts. The CSV
 * mode is the offline one, for a machine without Docker; it is never chosen on the run's
 * behalf. A database that cannot be reached is never swapped for the files: highlights quietly
 * written to a CSV would look lost from the database.
 */
final class Storage {
    static final String MODE_VARIABLE = "VARZINHO_STORAGE";
    static final String URL_VARIABLE = "VARZINHO_DATABASE_URL";
    static final String USER_VARIABLE = "VARZINHO_DATABASE_USER";
    static final String PASSWORD_VARIABLE = "VARZINHO_DATABASE_PASSWORD";

    static final String CSV_MODE = "csv";
    static final String DATABASE_MODE = "postgresql";

    // The same database, user and password docker-compose.yml creates when nothing overrides them.
    static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/varzinho";
    static final String DEFAULT_USER = "varzinho";
    static final String DEFAULT_PASSWORD = "varzinho";

    private static final String PASSWORD_PARAMETER = "(?i)([?&;]password=)[^&;]*";

    private final HighlightRepository highlights;
    private final GymRepository gym;
    private final OperatorStore operators;

    private Storage(HighlightRepository highlights, GymRepository gym, OperatorStore operators) {
        this.highlights = highlights;
        this.gym = gym;
        this.operators = operators;
    }

    /**
     * Where a run connects when it stores in PostgreSQL.
     *
     * @param url the JDBC address
     * @param user the database user
     * @param password the user's password; {@code null} when none is given
     */
    record Database(String url, String user, String password) {
        /** The address with any password it carries masked, safe to show and to log. */
        String shownUrl() {
            return url.replaceAll(PASSWORD_PARAMETER, "$1***");
        }

        @Override
        public String toString() {
            return user + " at " + shownUrl();
        }
    }

    /**
     * @param environment looks up a variable by name, answering {@code null} when it is unset
     * @param dataFolder where the CSV files live in the CSV mode
     * @throws RepositoryException if {@value #MODE_VARIABLE} names no known mode, if the CSV
     *         mode is asked for while a database is named too, or if the database has no user
     *         or cannot be reached; the message names the database, never its password
     */
    static Storage fromEnvironment(UnaryOperator<String> environment, Path dataFolder)
            throws RepositoryException {
        String mode = modeFrom(environment);
        if (CSV_MODE.equals(mode)) {
            return inCsvFiles(environment, dataFolder);
        }
        return inDatabase(databaseFrom(environment));
    }

    /**
     * The database a run in the PostgreSQL mode connects to: the one named by
     * {@value #URL_VARIABLE}, or the {@code docker-compose.yml} one when it is unset or blank.
     *
     * @param environment looks up a variable by name, answering {@code null} when it is unset
     * @return the address and credentials to connect with
     * @throws RepositoryException if a database is named without {@value #USER_VARIABLE}
     */
    static Database databaseFrom(UnaryOperator<String> environment) throws RepositoryException {
        String url = environment.apply(URL_VARIABLE);
        String user = environment.apply(USER_VARIABLE);
        String password = environment.apply(PASSWORD_VARIABLE);
        if (isBlank(url)) {
            return new Database(DEFAULT_URL, isBlank(user) ? DEFAULT_USER : user,
                    password == null ? DEFAULT_PASSWORD : password);
        }
        Database named = new Database(url, user, password);
        if (isBlank(user)) {
            throw new RepositoryException(URL_VARIABLE + " names " + named.shownUrl() + " but "
                    + USER_VARIABLE + " is not set");
        }
        return named;
    }

    private static String modeFrom(UnaryOperator<String> environment) throws RepositoryException {
        String asked = environment.apply(MODE_VARIABLE);
        if (isBlank(asked)) {
            return DATABASE_MODE;
        }
        String mode = asked.trim().toLowerCase(Locale.ROOT);
        if (!mode.equals(CSV_MODE) && !mode.equals(DATABASE_MODE)) {
            throw new RepositoryException(MODE_VARIABLE + " is '" + asked.trim() + "'; use "
                    + CSV_MODE + " or " + DATABASE_MODE);
        }
        return mode;
    }

    private static Storage inCsvFiles(UnaryOperator<String> environment, Path dataFolder)
            throws RepositoryException {
        // Both set is ambiguous: the operator would look for the highlights in the wrong place.
        if (!isBlank(environment.apply(URL_VARIABLE))) {
            throw new RepositoryException(MODE_VARIABLE + " asks for " + CSV_MODE + " but "
                    + URL_VARIABLE + " names a database; unset one of them");
        }
        return new Storage(new CsvHighlightRepository(dataFolder.resolve("highlights.csv")),
                new CsvGymRepository(dataFolder.resolve("gym.csv")),
                new CsvOperatorStore(dataFolder.resolve("operators.csv")));
    }

    private static Storage inDatabase(Database database) throws RepositoryException {
        try {
            return new Storage(
                    PostgresHighlightRepository.open(database.url(), database.user(), database.password()),
                    PostgresGymRepository.open(database.url(), database.user(), database.password()),
                    PostgresOperatorStore.open(database.url(), database.user(), database.password()));
        } catch (RepositoryException failure) {
            // The adapter's own message repeats the raw url, which may carry the password.
            throw new RepositoryException("could not reach the database at " + database.shownUrl()
                    + " (" + rootReason(failure) + ")", failure);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
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

    OperatorStore operators() {
        return operators;
    }
}
