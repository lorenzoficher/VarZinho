package br.edu.unipampa.varzinho.infrastructure.persistence;

import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.domain.capture.Camera;
import br.edu.unipampa.varzinho.domain.capture.FixedCamera;
import br.edu.unipampa.varzinho.domain.highlight.VideoClip;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.enums.Resolution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The gym file is open to new kinds of camera and closed to editing: a kind the
 * repository has never heard of is stored once a format for it is handed in.
 */
class CameraCsvFormatTest {

    @TempDir
    private Path directory;

    @Test
    void aNewKindOfCameraRoundTripsOnceItsFormatIsAdded() throws RepositoryException {
        List<CameraCsvFormat<?>> formats = new ArrayList<>(CameraCsvFormats.standard());
        formats.add(new ThermalCameraCsvFormat());
        CsvGymRepository repository = new CsvGymRepository(directory.resolve("gym.csv"), formats);
        Gym gym = new Gym("Arena", "Street 1");
        gym.addCourt(new Court(1));
        gym.installCamera(1, new ThermalCamera("thermal-1", 37));

        repository.save(gym);
        Gym loaded = repository.load().orElseThrow();

        ThermalCamera thermal = assertInstanceOf(ThermalCamera.class, loaded.findCourt(1).getCameras().get(0));
        assertEquals("thermal-1", thermal.getId());
        assertEquals(37, thermal.celsiusThreshold);
    }

    @Test
    void aKindWithNoFormatIsRefusedInsteadOfWrittenWrong() {
        CsvGymRepository repository = new CsvGymRepository(directory.resolve("gym.csv"));
        Gym gym = new Gym("Arena", "Street 1");
        gym.addCourt(new Court(1));
        gym.installCamera(1, new ThermalCamera("thermal-1", 37));

        assertThrows(IllegalArgumentException.class, () -> repository.save(gym));
    }

    @Test
    void twoFormatsForTheSameKindAreRefused() {
        List<CameraCsvFormat<?>> formats = List.of(new FixedCameraCsvFormat(), new FixedCameraCsvFormat());

        assertThrows(IllegalArgumentException.class,
                () -> new CsvGymRepository(directory.resolve("gym.csv"), formats));
    }

    @Test
    void aFormatCannotTakeAWordTheFileUsesForCourts() {
        assertThrows(IllegalArgumentException.class,
                () -> new CsvGymRepository(directory.resolve("gym.csv"), List.of(new CourtWordFormat())));
    }

    private static final class CourtWordFormat implements CameraCsvFormat<FixedCamera> {
        private final FixedCameraCsvFormat fixed = new FixedCameraCsvFormat();

        @Override
        public String kind() {
            return "court";
        }

        @Override
        public Class<FixedCamera> cameraClass() {
            return fixed.cameraClass();
        }

        @Override
        public int ownColumns() {
            return fixed.ownColumns();
        }

        @Override
        public List<String> ownFields(FixedCamera camera) {
            return fixed.ownFields(camera);
        }

        @Override
        public FixedCamera build(String id, String model, Resolution resolution, int bufferSeconds,
                                 List<String> ownFields) {
            return fixed.build(id, model, resolution, bufferSeconds, ownFields);
        }
    }

    private static final class ThermalCamera extends Camera {
        private final int celsiusThreshold;

        ThermalCamera(String id, int celsiusThreshold) {
            super(id, "Thermo", Resolution.HD, 30);
            this.celsiusThreshold = celsiusThreshold;
        }

        @Override
        public VideoClip captureLastSeconds(int seconds) {
            throw new UnsupportedOperationException("not needed to store the camera");
        }

        @Override
        public String describe() {
            return describeAs("thermal");
        }
    }

    private static final class ThermalCameraCsvFormat implements CameraCsvFormat<ThermalCamera> {
        @Override
        public String kind() {
            return "thermal";
        }

        @Override
        public Class<ThermalCamera> cameraClass() {
            return ThermalCamera.class;
        }

        @Override
        public int ownColumns() {
            return 1;
        }

        @Override
        public List<String> ownFields(ThermalCamera camera) {
            return List.of(String.valueOf(camera.celsiusThreshold));
        }

        @Override
        public ThermalCamera build(String id, String model, Resolution resolution, int bufferSeconds,
                                   List<String> ownFields) {
            return new ThermalCamera(id, Integer.parseInt(ownFields.get(0)));
        }
    }
}
