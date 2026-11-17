package br.edu.unipampa.varzinho.infrastructure.persistence;

import br.edu.unipampa.varzinho.application.port.OperatorStore;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.application.port.StorableOperators;
import br.edu.unipampa.varzinho.domain.people.Operator;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/** Keeps operator accounts in a CSV file without ever writing their plain passwords. */
public final class CsvOperatorStore implements OperatorStore {
    static final String HEADER = "name,username,passwordHash";

    private final Path file;

    public CsvOperatorStore(Path file) {
        if (file == null) throw new IllegalArgumentException("operators need a file to live in");
        this.file = file;
    }

    @Override
    public void save(Operator operator) throws RepositoryException {
        StorableOperators.requireStorable(operator);
        List<Operator> operators = readAll();
        if (operators.contains(operator)) {
            throw new IllegalArgumentException("username " + operator.getUsername() + " is already taken");
        }
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        operators.forEach(stored -> lines.add(format(stored)));
        lines.add(format(operator));
        write(lines);
    }

    @Override
    public Optional<Operator> findByUsername(String username) throws RepositoryException {
        if (username == null) return Optional.empty();
        String normalized = username.toLowerCase(Locale.ROOT);
        return readAll().stream()
                .filter(operator -> operator.getUsername().equals(normalized))
                .findFirst();
    }

    @Override
    public boolean hasAny() throws RepositoryException {
        return !readAll().isEmpty();
    }

    private List<Operator> readAll() throws RepositoryException {
        if (!Files.exists(file)) return List.of();
        List<String> lines;
        try {
            lines = Files.readAllLines(file);
        } catch (IOException cause) {
            throw new RepositoryException("could not read the operators " + file, cause);
        }
        if (lines.isEmpty()) {
            throw new RepositoryException("the operator file " + file + " is empty");
        }
        if (!HEADER.equals(lines.get(0))) {
            throw new RepositoryException("the operator file " + file + " does not start with the header");
        }
        List<Operator> operators = new ArrayList<>();
        Set<String> usernames = new HashSet<>();
        for (int index = 1; index < lines.size(); index++) {
            int lineNumber = index + 1;
            String[] fields = lines.get(index).split(",", -1);
            if (fields.length != 3) {
                throw damaged(lineNumber, fields.length + " columns where 3 were expected", null);
            }
            try {
                Operator operator = Operator.restore(fields[0], fields[1], fields[2]);
                if (!usernames.add(operator.getUsername())) {
                    throw new IllegalArgumentException("duplicate username " + operator.getUsername());
                }
                operators.add(operator);
            } catch (IllegalArgumentException invalid) {
                throw damaged(lineNumber, invalid.getMessage(), invalid);
            }
        }
        return List.copyOf(operators);
    }

    private static String format(Operator operator) {
        return String.join(",", operator.getName(), operator.getUsername(), operator.getPasswordHash());
    }

    private static RepositoryException damaged(int lineNumber, String reason, Throwable cause) {
        String message = "line " + lineNumber + " of the operator file cannot be read: " + reason;
        return cause == null ? new RepositoryException(message) : new RepositoryException(message, cause);
    }

    private void write(List<String> lines) throws RepositoryException {
        try {
            Path directory = file.toAbsolutePath().getParent();
            Files.createDirectories(directory);
            Path temporary = Files.createTempFile(directory, "operators", ".tmp");
            try {
                Files.write(temporary, lines);
                try {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException unsupported) {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException cause) {
            throw new RepositoryException("could not write the operators " + file, cause);
        }
    }
}
