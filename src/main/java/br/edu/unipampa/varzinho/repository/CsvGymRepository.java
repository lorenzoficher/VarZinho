package br.edu.unipampa.varzinho.repository;

import br.edu.unipampa.varzinho.domain.capture.Camera;
import br.edu.unipampa.varzinho.domain.capture.FixedCamera;
import br.edu.unipampa.varzinho.domain.capture.PtzCamera;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.enums.CameraStatus;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.exception.DomainException;
import br.edu.unipampa.varzinho.exception.RepositoryException;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
    private static final String FIXED = "fixed";
    private static final String PTZ = "ptz";

    private final Path file;

    public CsvGymRepository(Path file) {
        if (file == null) {
            throw new IllegalArgumentException("the gym needs a file to live in");
        }
        this.file = file;
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
        courts.sort(Comparator.comparingInt(Court::getNumber));
        for (Court court : courts) {
            lines.add(line(COURT, String.valueOf(court.getNumber())));
        }
        for (Court court : courts) {
            for (Camera camera : court.getCameras()) {
                lines.add(format(court.getNumber(), camera));
            }
        }
        write(lines);
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

    private static void install(Gym gym, String[] fields, int lineNumber) throws RepositoryException {
        Camera camera;
        if (FIXED.equals(fields[0])) {
            require(fields, 8, lineNumber);
            camera = new FixedCamera(fields[2], fields[3], Resolution.valueOf(fields[4]),
                    Integer.parseInt(fields[5]), Integer.parseInt(fields[7]));
        } else if (PTZ.equals(fields[0])) {
            require(fields, 10, lineNumber);
            PtzCamera ptz = new PtzCamera(fields[2], fields[3], Resolution.valueOf(fields[4]),
                    Integer.parseInt(fields[5]));
            ptz.moveTo(Integer.parseInt(fields[7]), Integer.parseInt(fields[8]), Integer.parseInt(fields[9]));
            camera = ptz;
        } else {
            throw unreadable(lineNumber, "an unknown kind '" + fields[0] + "'");
        }
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

    private static String format(int courtNumber, Camera camera) {
        String court = String.valueOf(courtNumber);
        String resolution = camera.getResolution().name();
        String buffer = String.valueOf(camera.getBufferSeconds());
        String status = camera.getStatus().name();
        if (camera instanceof PtzCamera) {
            PtzCamera ptz = (PtzCamera) camera;
            return line(PTZ, court, camera.getId(), camera.getModel(), resolution, buffer, status,
                    String.valueOf(ptz.getPan()), String.valueOf(ptz.getTilt()), String.valueOf(ptz.getZoom()));
        }
        if (camera instanceof FixedCamera) {
            return line(FIXED, court, camera.getId(), camera.getModel(), resolution, buffer, status,
                    String.valueOf(((FixedCamera) camera).getAngle()));
        }
        throw new IllegalArgumentException("cannot store a camera of kind " + camera.getClass().getSimpleName());
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
