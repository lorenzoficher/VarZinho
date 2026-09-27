package br.edu.unipampa.varzinho;

import br.edu.unipampa.varzinho.domain.capture.FixedCamera;
import br.edu.unipampa.varzinho.domain.capture.Frame;
import br.edu.unipampa.varzinho.domain.capture.PtzCamera;
import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.domain.people.Athlete;
import br.edu.unipampa.varzinho.domain.people.Operator;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.exception.NoActiveCameraException;
import br.edu.unipampa.varzinho.exception.RepositoryException;
import br.edu.unipampa.varzinho.repository.CsvHighlightRepository;
import br.edu.unipampa.varzinho.repository.HighlightRepository;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class ConsoleDemo {
    private ConsoleDemo() { }

    public static void main(String[] args) {
        run(Path.of("data", "highlights.csv"));
    }

    public static void run(Path archive) {
        if (archive == null) throw new IllegalArgumentException("a console demo needs an archive path");

        Gym gym = new Gym("VarZinho Arena", "100 Sports Avenue");
        Court court = new Court(1);
        Court inactiveCourt = new Court(2);
        gym.addCourt(court);
        gym.addCourt(inactiveCourt);
        Athlete athlete = new Athlete("Ana Silva", "ATH-1", LocalDate.of(2000, 5, 10), 10, "Forward");
        Operator operator = new Operator("Otavio Lima", "OP-1", LocalDate.of(1990, 3, 3), "B-7", "Evening");
        gym.registerPerson(athlete);
        gym.registerPerson(operator);

        FixedCamera fixed = new FixedCamera("fixed-1", "Fixed Pro", Resolution.FULL_HD, 30, 45);
        PtzCamera ptz = new PtzCamera("ptz-1", "PTZ Pro", Resolution.HD, 30);
        court.installCamera(fixed);
        court.installCamera(ptz);
        fixed.startRecording();
        ptz.startRecording();
        Instant lastFrameAt = Instant.now();
        for (int second = 0; second < Court.DEFAULT_CAPTURE_SECONDS; second++) {
            long secondsBeforeLastFrame = Court.DEFAULT_CAPTURE_SECONDS - 1L - second;
            court.record(new Frame(lastFrameAt.minusSeconds(secondsBeforeLastFrame), second));
        }

        Highlight captured = court.triggerCapture();
        try {
            HighlightRepository repository = new CsvHighlightRepository(archive);
            repository.save(captured);
            HighlightRepository reopenedRepository = new CsvHighlightRepository(archive);
            Highlight reloaded = reopenedRepository.findById(captured.getId())
                    .orElseThrow(() -> new RepositoryException(
                            "Saved highlight " + captured.getId() + " could not be reloaded"));
            List<Highlight> courtArchive = reopenedRepository.findByCourt(court.getNumber());

            System.out.println("Gym: " + gym.getName());
            System.out.println("Registered: " + athlete.identify() + " and " + operator.identify());
            System.out.println("Reloaded highlight: " + reloaded.describe());
            System.out.println("Existing archive for court 1 searched by " + operator.identify()
                    + " after a request from " + athlete.identify() + " ("
                    + courtArchive.size() + " total entries):");
            courtArchive.forEach(item -> System.out.println(" - " + item.describe()));
        } catch (RepositoryException exception) {
            System.err.println("Archive error: " + exception.getMessage());
        }

        try {
            inactiveCourt.triggerCapture();
        } catch (NoActiveCameraException exception) {
            System.out.println("Expected error: " + exception.getMessage());
        }
    }
}
