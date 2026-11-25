package br.edu.unipampa.varzinho.infrastructure.persistence;

import br.edu.unipampa.varzinho.application.port.GymRepository;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.domain.capture.Camera;
import br.edu.unipampa.varzinho.domain.capture.StreamCamera;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.enums.CameraStatus;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.exception.DomainException;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Keeps the gym's structure in a CSV file, one line per gym, court or camera.
 *
 * <p>The file format is private to this class. Loading rebuilds the gym only through the
 * domain's own behaviour — adding courts, installing cameras, switching and aiming them —
 * so a file cannot produce a gym the domain would have refused. A line that cannot be
 * read fails the whole load: half a gym would be a gym nobody built.
 */
public final class CsvGymRepository implements GymRepository {

    static final String HEADER = "kind,values";
    private static final String SEPARATOR = ",";
    private static final String GYM = "gym";
    private static final String COURT = "court";
    // A court line once held only the number; files written then must keep loading.
    private static final int UNLABELLED_COURT_COLUMNS = 2;
    private static final int COURT_COLUMNS = 4;
    private static final int COMMON_COLUMNS = 7;

    private final Path file;
    private final Map<String, CameraCsvFormat<?>> formatsByKind;
    private final Map<Class<?>, CameraCsvFormat<?>> formatsByClass;

    /**
     * Keeps the gym in {@code file}, with the formats for the domain's standard cameras.
     *
     * @param file where the gym is stored; it need not exist until the first save
     * @throws IllegalArgumentException if the file is missing
     */
    public CsvGymRepository(Path file) {
        this(file, CameraCsvFormats.standard());
    }

    /**
     * Registers one format per kind and exact camera class. Gym and court are reserved
     * kinds because they describe the structure, never a camera.
     *
     * @param file where the gym is stored; it need not exist until the first save
     * @param formats one format per camera class the gym may hold
     * @throws IllegalArgumentException if the file or the formats are missing, or if two
     *         formats share a kind or a class, or one uses a reserved or malformed kind
     */
    public CsvGymRepository(Path file, List<CameraCsvFormat<?>> formats) {
        if (file == null) {
            throw new IllegalArgumentException("the gym needs a file to live in");
        }
        this.file = file;
        if (formats == null) throw new IllegalArgumentException("the gym needs its camera formats");
        Map<String, CameraCsvFormat<?>> byKind = new HashMap<>();
        Map<Class<?>, CameraCsvFormat<?>> byClass = new HashMap<>();
        for (CameraCsvFormat<?> format : formats) {
            if (format == null || format.kind() == null || format.kind().isBlank()
                    || format.cameraClass() == null || format.ownColumns() < 0) {
                throw new IllegalArgumentException("a camera format needs a kind, class and nonnegative column count");
            }
            String kind = format.kind();
            line(kind);
            if (GYM.equals(kind) || COURT.equals(kind)) {
                throw new IllegalArgumentException("a camera format cannot use the reserved kind " + kind);
            }
            if (byKind.putIfAbsent(kind, format) != null) {
                throw new IllegalArgumentException("two camera formats use the kind " + kind);
            }
            if (byClass.putIfAbsent(format.cameraClass(), format) != null) {
                throw new IllegalArgumentException("two camera formats use the class " + format.cameraClass().getName());
            }
        }
        formatsByKind = Map.copyOf(byKind);
        formatsByClass = Map.copyOf(byClass);
    }

    /**
     * {@inheritDoc}
     *
     * <p>A comma or a line break inside a text would split its line on reload, so it is
     * refused before anything is written.
     */
    @Override
    public void save(Gym gym) throws RepositoryException {
        if (gym == null) {
            throw new IllegalArgumentException("there is no gym to save");
        }
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        lines.add(line(GYM, gym.getName(), gym.getAddress()));
        List<Court> courts = new ArrayList<>(gym.getCourts());
        courts.removeIf(CsvGymRepository::holdsOnlyStreams);
        courts.sort(Comparator.comparingInt(Court::getNumber));
        for (Court court : courts) {
            lines.add(line(COURT, String.valueOf(court.getNumber()),
                    court.getDisplayName().orElse(""), court.getSport().orElse("")));
        }
        for (Court court : courts) {
            for (Camera camera : court.getCameras()) {
                if (!StreamCamera.class.isInstance(camera)) {
                    lines.add(format(court.getNumber(), camera));
                }
            }
        }
        write(lines);
    }

    // A stream camera is runtime equipment: it is installed afresh on every start, and
    // storing it would make this repository build the tool that writes its clips. A
    // court that exists only to hold one goes with it, or every start would add another.
    private static boolean holdsOnlyStreams(Court court) {
        List<Camera> cameras = court.getCameras();
        return !cameras.isEmpty() && cameras.stream().allMatch(StreamCamera.class::isInstance);
    }

    @Override
    public Optional<Gym> load() throws RepositoryException {
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(file);
        } catch (IOException cause) {
            throw new RepositoryException("could not read the gym " + file, cause);
        }
        if (lines.isEmpty()) {
            return Optional.empty();
        }
        if (!HEADER.equals(lines.get(0))) {
            throw new RepositoryException("the gym file " + file + " does not start with the header");
        }
        Gym gym = null;
        for (int index = 1; index < lines.size(); index++) {
            String text = lines.get(index);
            if (text.isBlank()) {
                continue;
            }
            int lineNumber = index + 1;
            String[] fields = text.split(SEPARATOR, -1);
            try {
                if (GYM.equals(fields[0])) {
                    if (gym != null) throw unreadable(lineNumber, "a second gym");
                    require(fields, 3, lineNumber);
                    gym = new Gym(fields[1], fields[2]);
                } else if (gym == null) {
                    throw unreadable(lineNumber, "a court or camera before the gym line");
                } else if (COURT.equals(fields[0])) {
                    gym.addCourt(court(fields, lineNumber));
                } else {
                    install(gym, fields, lineNumber);
                }
            } catch (IllegalArgumentException | IllegalStateException | DomainException refused) {
                // The domain refusing a line is as much damage as a line that does not parse.
                throw new RepositoryException("line " + lineNumber + " of " + file + ": " + refused.getMessage(),
                        refused);
            }
        }
        if (gym == null) {
            throw new RepositoryException("the gym file " + file + " has no gym line");
        }
        return Optional.of(gym);
    }

    private static Court court(String[] fields, int lineNumber) throws RepositoryException {
        if (fields.length != UNLABELLED_COURT_COLUMNS) {
            require(fields, COURT_COLUMNS, lineNumber);
        }
        Court court = new Court(Integer.parseInt(fields[1]));
        if (fields.length == COURT_COLUMNS) {
            court.rename(fields[2]);
            court.assignSport(fields[3]);
        }
        return court;
    }

    private void install(Gym gym, String[] fields, int lineNumber) throws RepositoryException {
        CameraCsvFormat<?> format = formatsByKind.get(fields[0]);
        if (format == null) {
            throw unreadable(lineNumber, "an unknown kind '" + fields[0] + "'");
        }
        require(fields, COMMON_COLUMNS + format.ownColumns(), lineNumber);
        Camera camera = format.build(fields[2], fields[3], Resolution.valueOf(fields[4]),
                Integer.parseInt(fields[5]), List.copyOf(Arrays.asList(fields).subList(COMMON_COLUMNS, fields.length)));
        int courtNumber = Integer.parseInt(fields[1]);
        if (gym.findCourt(courtNumber).isEmpty()) {
            throw unreadable(lineNumber, "a camera on court " + courtNumber + ", which the file never declared");
        }
        gym.installCamera(courtNumber, camera);
        restore(camera, CameraStatus.valueOf(fields[6]));
    }

    private static void restore(Camera camera, CameraStatus status) {
        switch (status) {
            case ACTIVE: camera.startRecording(); break;
            case MAINTENANCE: camera.sendToMaintenance(); break;
            case INACTIVE: break;
            default: throw new IllegalArgumentException("no way to restore a camera to " + status);
        }
    }

    private String format(int courtNumber, Camera camera) {
        CameraCsvFormat<?> format = formatsByClass.get(camera.getClass());
        if (format == null) {
            throw new IllegalArgumentException("cannot store a camera of kind " + camera.getClass().getSimpleName());
        }
        List<String> fields = new ArrayList<>(List.of(format.kind(), String.valueOf(courtNumber),
                camera.getId(), camera.getModel(), camera.getResolution().name(),
                String.valueOf(camera.getBufferSeconds()), camera.getStatus().name()));
        fields.addAll(ownFields(format, camera));
        return line(fields.toArray(String[]::new));
    }

    private static <C extends Camera> List<String> ownFields(CameraCsvFormat<C> format, Camera camera) {
        List<String> fields = format.ownFields(format.cameraClass().cast(camera));
        if (fields == null || fields.size() != format.ownColumns()) {
            throw new IllegalArgumentException("the camera format " + format.kind() + " returned the wrong column count");
        }
        return fields;
    }

    private static String line(String... fields) {
        for (String field : fields) {
            if (field.contains(SEPARATOR) || field.contains("\n") || field.contains("\r")) {
                throw new IllegalArgumentException("cannot store a value with a comma or line break: " + field);
            }
        }
        return String.join(SEPARATOR, fields);
    }

    private static void require(String[] fields, int columns, int lineNumber) throws RepositoryException {
        if (fields.length != columns) {
            throw unreadable(lineNumber, fields.length + " columns where " + columns + " were expected");
        }
    }

    private static RepositoryException unreadable(int lineNumber, String what) {
        return new RepositoryException("line " + lineNumber + " holds " + what);
    }

    private void write(List<String> lines) throws RepositoryException {
        try {
            Path directory = file.toAbsolutePath().getParent();
            Files.createDirectories(directory);
            // Writing in place truncates first, so a crash mid-write would lose the whole gym.
            Path temporary = Files.createTempFile(directory, "gym", ".tmp");
            try {
                Files.write(temporary, lines);
                try {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException unsupported) {
                    // Some synced or network folders refuse an atomic move; a plain replace still beats failing the save.
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException cause) {
            throw new RepositoryException("could not write the gym " + file, cause);
        }
    }
}
