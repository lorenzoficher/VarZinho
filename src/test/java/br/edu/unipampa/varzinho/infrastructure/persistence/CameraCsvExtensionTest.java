package br.edu.unipampa.varzinho.infrastructure.persistence;

import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.domain.capture.Camera;
import br.edu.unipampa.varzinho.domain.capture.FixedCamera;
import br.edu.unipampa.varzinho.domain.capture.PtzCamera;
import br.edu.unipampa.varzinho.domain.highlight.VideoClip;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.enums.CameraStatus;
import br.edu.unipampa.varzinho.enums.Resolution;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CameraCsvExtensionTest {
    @TempDir
    private Path directory;

    @Test
    void aNewCameraRoundTripsWhenItsFormatIsRegistered() throws Exception {
        Path file = directory.resolve("gym.csv");
        CsvGymRepository repository = new CsvGymRepository(file, List.of(new TaggedFormat("tagged")));
        Gym gym = gymWithTaggedCamera();

        repository.save(gym);
        Camera restored = repository.load().orElseThrow().findCourt(4).getCameras().get(0);

        TaggedCamera tagged = assertInstanceOf(TaggedCamera.class, restored);
        assertEquals(7, tagged.channel);
        assertEquals("tagged-1", tagged.getId());
        assertEquals("Custom model", tagged.getModel());
        assertEquals(Resolution.HD, tagged.getResolution());
        assertEquals(45, tagged.getBufferSeconds());
        assertEquals(CameraStatus.ACTIVE, tagged.getStatus());
        assertEquals(List.of("kind,values", "gym,Arena,Rua A", "court,4",
                "tagged,4,tagged-1,Custom model,HD,45,ACTIVE,7"), Files.readAllLines(file));
    }

    @Test
    void anUnregisteredCameraIsRefusedBeforeTheSavedGymChanges() throws Exception {
        Path file = directory.resolve("gym.csv");
        CsvGymRepository repository = new CsvGymRepository(file);
        repository.save(new Gym("Original", "Rua A"));
        List<String> before = Files.readAllLines(file);

        assertThrows(IllegalArgumentException.class, () -> repository.save(gymWithTaggedCamera()));

        assertEquals(before, Files.readAllLines(file));
    }

    @Test
    void aStoredKindWithoutARegisteredFormatIsReportedWithItsLine() throws Exception {
        Path file = directory.resolve("gym.csv");
        new CsvGymRepository(file, List.of(new TaggedFormat("tagged"))).save(gymWithTaggedCamera());

        RepositoryException failure = assertThrows(RepositoryException.class,
                () -> new CsvGymRepository(file).load());

        assertTrue(failure.getMessage().contains("line 4"));
        assertTrue(failure.getMessage().contains("tagged"));
    }

    @Test
    void twoFormatsCannotUseTheSameKindEvenForDifferentClasses() {
        assertThrows(IllegalArgumentException.class, () -> new CsvGymRepository(directory.resolve("gym.csv"),
                List.of(new FixedCameraCsvFormat(), new TaggedFormat("fixed"))));
    }

    @Test
    void twoFormatsCannotUseTheSameCameraClassEvenForDifferentKinds() {
        assertThrows(IllegalArgumentException.class, () -> new CsvGymRepository(directory.resolve("gym.csv"),
                List.of(new TaggedFormat("first"), new TaggedFormat("second"))));
    }

    @Test
    void gymAndCourtAreReservedForStructureLines() {
        for (String kind : List.of("gym", "court")) {
            assertThrows(IllegalArgumentException.class, () -> new CsvGymRepository(directory.resolve("gym.csv"),
                    List.of(new TaggedFormat(kind))));
        }
    }

    @Test
    void changingTheCallersListDoesNotChangeTheRegisteredFormats() throws Exception {
        List<CameraCsvFormat<?>> formats = new ArrayList<>(List.of(new TaggedFormat("tagged")));
        CsvGymRepository repository = new CsvGymRepository(directory.resolve("gym.csv"), formats);
        formats.clear();

        repository.save(gymWithTaggedCamera());

        assertInstanceOf(TaggedCamera.class, repository.load().orElseThrow().findCourt(4).getCameras().get(0));
    }

    @Test
    void aFormatRejectsMissingOrExtraOwnColumnsBeforeBuildingTheCamera() throws Exception {
        Path file = directory.resolve("gym.csv");
        CsvGymRepository repository = new CsvGymRepository(file, List.of(new TaggedFormat("tagged")));
        for (String cameraLine : List.of("tagged,4,tagged-1,Custom model,HD,45,ACTIVE",
                "tagged,4,tagged-1,Custom model,HD,45,ACTIVE,7,extra")) {
            Files.write(file, List.of("kind,values", "gym,Arena,Rua A", "court,4", cameraLine));

            RepositoryException failure = assertThrows(RepositoryException.class, repository::load);

            assertTrue(failure.getMessage().contains("line 4"));
        }
    }

    @Test
    void existingFixedAndPtzLinesLoadAndSaveWithoutChangingTheFileFormat() throws Exception {
        Path file = directory.resolve("gym.csv");
        List<String> original = List.of("kind,values", "gym,Arena,Rua A", "court,1",
                "fixed,1,fixed-1,Bullet,FULL_HD,30,ACTIVE,45",
                "ptz,1,ptz-1,Dome,HD,60,MAINTENANCE,90,-20,3");
        Files.write(file, original);
        CsvGymRepository repository = new CsvGymRepository(file);

        Gym gym = repository.load().orElseThrow();

        FixedCamera fixed = assertInstanceOf(FixedCamera.class, gym.findCourt(1).getCameras().get(0));
        assertEquals(45, fixed.getAngle());
        assertEquals(CameraStatus.ACTIVE, fixed.getStatus());
        PtzCamera ptz = assertInstanceOf(PtzCamera.class, gym.findCourt(1).getCameras().get(1));
        assertEquals(90, ptz.getPan());
        assertEquals(-20, ptz.getTilt());
        assertEquals(3, ptz.getZoom());
        assertEquals(CameraStatus.MAINTENANCE, ptz.getStatus());
        repository.save(gym);
        assertEquals(original, Files.readAllLines(file));
    }

    private static Gym gymWithTaggedCamera() {
        Gym gym = new Gym("Arena", "Rua A");
        gym.addCourt(new Court(4));
        TaggedCamera camera = new TaggedCamera("tagged-1", "Custom model", Resolution.HD, 45, 7);
        camera.startRecording();
        gym.installCamera(4, camera);
        return gym;
    }

    private static final class TaggedCamera extends Camera {
        private final int channel;

        private TaggedCamera(String id, String model, Resolution resolution, int bufferSeconds, int channel) {
            super(id, model, resolution, bufferSeconds);
            this.channel = channel;
        }

        @Override
        public VideoClip captureLastSeconds(int seconds) {
            var window = recordedWindow(seconds);
            return clipFrom(clipName(window.get(window.size() - 1)) + ".mp4", seconds);
        }

        @Override
        public String describe() {
            return describeAs("tagged");
        }
    }

    private static final class TaggedFormat implements CameraCsvFormat<TaggedCamera> {
        private final String kind;

        private TaggedFormat(String kind) {
            this.kind = kind;
        }

        @Override
        public String kind() { return kind; }

        @Override
        public Class<TaggedCamera> cameraClass() { return TaggedCamera.class; }

        @Override
        public int ownColumns() { return 1; }

        @Override
        public List<String> ownFields(TaggedCamera camera) { return List.of(String.valueOf(camera.channel)); }

        @Override
        public TaggedCamera build(String id, String model, Resolution resolution, int bufferSeconds,
                                  List<String> ownFields) {
            return new TaggedCamera(id, model, resolution, bufferSeconds, Integer.parseInt(ownFields.get(0)));
        }
    }
}
