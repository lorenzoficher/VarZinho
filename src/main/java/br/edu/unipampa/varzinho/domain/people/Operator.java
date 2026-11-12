package br.edu.unipampa.varzinho.domain.people;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** The staff account that operates VarZinho. Its plain password is never retained. */
public final class Operator {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String HASH_PREFIX = "pbkdf2-sha256";
    private static final int ITERATIONS = 120_000;
    private static final int MAX_ACCEPTED_ITERATIONS = 1_000_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int KEY_LENGTH_BYTES = KEY_LENGTH_BITS / Byte.SIZE;
    private static final int SALT_BYTES = 16;
    private static final int STORED_PARTS = 4;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final String name;
    private final String username;
    private final String passwordHash;

    public Operator(String name, String username, String password) {
        this(validateName(name), normalizeUsername(username), hash(validatePassword(password)));
    }

    private Operator(String name, String username, EncodedHash passwordHash) {
        this.name = name;
        this.username = username;
        this.passwordHash = passwordHash.value();
    }

    /** Rebuilds an account exactly as a repository stored it. */
    public static Operator restore(String name, String username, String passwordHash) {
        return new Operator(validateName(name), normalizeUsername(username), validateStoredHash(passwordHash));
    }

    public boolean checksPassword(String password) {
        if (password == null) return false;
        StoredHash stored = parse(passwordHash);
        byte[] candidate = derive(password.toCharArray(), stored.salt(), stored.iterations());
        return MessageDigest.isEqual(stored.hash(), candidate);
    }

    public String getName() {
        return name;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Operator operator && username.equals(operator.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("an operator needs a name");
        }
        return name;
    }

    private static String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("an operator needs a username");
        }
        if (username.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("a username cannot contain whitespace");
        }
        return username.toLowerCase(Locale.ROOT);
    }

    private static String validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("a password needs at least 8 characters");
        }
        if (password.chars().noneMatch(Character::isUpperCase)) {
            throw new IllegalArgumentException("a password needs an uppercase letter");
        }
        if (password.chars().noneMatch(Character::isLowerCase)) {
            throw new IllegalArgumentException("a password needs a lowercase letter");
        }
        if (password.chars().noneMatch(Character::isDigit)) {
            throw new IllegalArgumentException("a password needs a digit");
        }
        if (password.chars().noneMatch(character -> !Character.isLetterOrDigit(character)
                && !Character.isWhitespace(character))) {
            throw new IllegalArgumentException("a password needs a symbol");
        }
        return password;
    }

    private static EncodedHash hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = derive(password.toCharArray(), salt, ITERATIONS);
        Base64.Encoder encoder = Base64.getEncoder();
        return new EncodedHash(HASH_PREFIX + "$" + ITERATIONS + "$" + encoder.encodeToString(salt)
                + "$" + encoder.encodeToString(hash));
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        PBEKeySpec specification = new PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(specification).getEncoded();
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException("PBKDF2 is not available in this Java runtime", unavailable);
        } catch (InvalidKeySpecException invalid) {
            throw new IllegalArgumentException("the stored password hash is invalid", invalid);
        } finally {
            specification.clearPassword();
        }
    }

    private static EncodedHash validateStoredHash(String passwordHash) {
        parse(passwordHash);
        return new EncodedHash(passwordHash);
    }

    private static StoredHash parse(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("an operator needs a stored password hash");
        }
        String[] parts = passwordHash.split("\\$", -1);
        if (parts.length != STORED_PARTS || !HASH_PREFIX.equals(parts[0])) {
            throw new IllegalArgumentException("the stored password hash has an unsupported format");
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] hash = Base64.getDecoder().decode(parts[3]);
            if (iterations <= 0 || iterations > MAX_ACCEPTED_ITERATIONS
                    || salt.length != SALT_BYTES || hash.length != KEY_LENGTH_BYTES) {
                throw new IllegalArgumentException("the stored password hash has invalid parameters");
            }
            return new StoredHash(iterations, salt, hash);
        } catch (IllegalArgumentException invalid) {
            throw new IllegalArgumentException("the stored password hash has invalid parameters", invalid);
        }
    }

    private record StoredHash(int iterations, byte[] salt, byte[] hash) { }
    private record EncodedHash(String value) { }
}
