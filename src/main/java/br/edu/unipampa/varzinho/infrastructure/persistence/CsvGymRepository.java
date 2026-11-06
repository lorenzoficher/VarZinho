package br.edu.unipampa.varzinho.infrastructure.persistence;

import br.edu.unipampa.varzinho.application.port.GymRepository;
import br.edu.unipampa.varzinho.domain.capture.Camera;
import br.edu.unipampa.varzinho.domain.capture.StreamCamera;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.enums.CameraStatus;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.exception.DomainException;
import br.edu.unipampa.varzinho.application.port.RepositoryException;

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
 *
 * <p>It knows no kind of camera by name. Each kind is written and read by its own
 * {@link CameraCsvFormat}, looked up by the camera's class on save and by the line's
 * first column on load.
 */
public final class CsvGymRepository implements GymRepository {

    static final String HEADER = "kind,values";
    private static final String SEPARATOR = ",";
    private static final String GYM = "gym";
    private static final String COURT = "court";
    // kind, court, id, model, resolution, buffer seconds, status
    private static final int SHARED_CAMERA_COLUMNS = 7;

    private final Path file;
    private final Map<String, CameraCsvFormat<?>> formatsByKind = new HashMap<>();
    private final Map<Class<?>, CameraCsvFormat<?>> formatsByClass = new HashMap<>();

    /** A gym file holding the camera kinds of {@link CameraCsvFormats#standard()}. */
    public CsvGymRepository(Path file) {
        this(file, CameraCsvFormats.standard());
    }

    /**
     * @param formats one per kind of camera this file stores
     * @throws IllegalArgumentException if two formats claim the same kind or camera class,
     *     or a kind is a word the file already uses for something else
     */
    public CsvGymRepository(Path file, List<CameraCsvFormat<?>> formats) {
        if (file == null) {
            throw new IllegalArgumentException("the gym needs a file to live in");
        }
        if (formats == null) {
            throw new IllegalArgumentException("the gym file needs to know how to store its cameras");
        }
        this.file = file;
        for (CameraCsvFormat<?> format : formats) {
            String kind = format.kind();
            if (GYM.equals(kind) || COURT.equals(kind) || formatsByKind.containsKey(kind)
                    || formatsByClass.containsKey(format.cameraClass())) {
                throw new IllegalArgumentException("two ways to store the camera kind '" + kind + "'");
            }
            formatsByKind.put(kind, format);
            formatsByClass.put(format.cameraClass(), format);
        }
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
            lines.add(line(COURT, String.valueOf(court.getNumber())));
        }
        for (Court court : courts) {
            for (Camera camera : court.getCameras()) {
                if (!(camera instanceof StreamCamera)) {
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
                    require(fields, 2, lineNumber);
                    gym.addCourt(new Court(Integer.parseInt(fields[1])));
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

    private void install(Gym gym, String[] fields, int lineNumber) throws RepositoryException {
        CameraCsvFormat<?> format = formatsByKind.get(fields[0]);
        if (format == null) {
            throw unreadable(lineNumber, "an unknown kind '" + fields[0] + "'");
        }
        require(fields, SHARED_CAMERA_COLUMNS + format.ownColumns(), lineNumber);
        Camera camera = format.build(fields[2], fields[3], Resolution.valueOf(fields[4]),
                Integer.parseInt(fields[5]), Arrays.asList(fields).subList(SHARED_CAMERA_COLUMNS, fields.length));
        int courtNumber = Integer.parseInt(fields[1]);
        if (gym.findCourt(courtNumber) == null) {
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
        List<String> fields = new ArrayList<>(List.of(format.kind(), String.valueOf(courtNumber), camera.getId(),
                camera.getModel(), camera.getResolution().name(), String.valueOf(camera.getBufferSeconds()),
                camera.getStatus().name()));
        fields.addAll(ownFields(format, camera));
        return line(fields.toArray(new String[0]));
    }

    // The format was found by this camera's class, so the cast cannot fail.
    private static <C extends Camera> List<String> ownFields(CameraCsvFormat<C> format, Camera camera) {
        return format.ownFields(format.cameraClass().cast(camera));
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
