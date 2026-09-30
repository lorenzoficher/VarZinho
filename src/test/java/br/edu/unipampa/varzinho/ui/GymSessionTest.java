package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.domain.capture.FixedCamera;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.repository.CsvGymRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GymSessionTest {

    @TempDir
    private Path directory;

    private final List<String> errors = new ArrayList<>();

    @Test
    void withNothingSavedTheSessionOpensOnTheSampleGym() {
        GymSession session = GymSession.start(new CsvGymRepository(directory.resolve("gym.csv")), errors::add);

        assertEquals(SampleGym.build().getName(), session.gym().getName());
        assertEquals(2, session.gym().getCourts().size());
        assertTrue(errors.isEmpty());
    }

    @Test
    void whatASessionSavedIsWhatTheNextSessionOpensOn() {
        Path file = directory.resolve("gym.csv");
        GymSession first = GymSession.start(new CsvGymRepository(file), errors::add);
        first.gym().addCourt(new Court(3));
        first.gym().installCamera(3, new FixedCamera("fixed-9", "Bullet", Resolution.HD, 30, 10));
        first.save(errors::add);

        GymSession second = GymSession.start(new CsvGymRepository(file), errors::add);

        assertEquals(3, second.gym().getCourts().size());
        assertEquals("fixed-9", second.gym().findCourt(3).getCameras().get(0).getId());
        assertTrue(errors.isEmpty());
    }

    @Test
    void aDamagedFileIsReportedAndLeftUntouchedWhileTheSessionRunsOnTheSample() throws IOException {
        Path file = directory.resolve("gym.csv");
        Files.write(file, List.of("this is not a gym file"));

        GymSession session = GymSession.start(new CsvGymRepository(file), errors::add);
        session.gym().addCourt(new Court(3));
        session.save(errors::add);

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).startsWith("The saved gym could not be read"), errors.get(0));
        assertEquals(SampleGym.build().getName(), session.gym().getName());
        assertEquals(List.of("this is not a gym file"), Files.readAllLines(file));
    }

    @Test
    void aSaveTheStorageRefusesIsReported() {
        GymSession session = GymSession.start(new CsvGymRepository(directory.resolve("gym.csv")), errors::add);
        session.gym().installCamera(1, new FixedCamera("fixed-5", "Bullet, 2nd gen", Resolution.HD, 30, 0));

        session.save(errors::add);

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).startsWith("The gym could not be saved"), errors.get(0));
    }
}
