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
import br.edu.unipampa.varzinho.repository.HighlightRepository;

import java.time.Instant;
import java.time.LocalDate;

public final class ConsoleDemo {
    private ConsoleDemo() { }

    public static void run(HighlightRepository repository) {
        if (repository == null) throw new IllegalArgumentException("a console demo needs a repository");
        try {
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
            for (int second = 0; second < Court.DEFAULT_CAPTURE_SECONDS; second++) {
                court.record(new Frame(Instant.now().plusSeconds(second), second));
            }

            Highlight captured = court.triggerCapture();
            repository.save(captured);
            Highlight reloaded = repository.findById(captured.getId()).orElseThrow();

            System.out.println("Gym: " + gym.getName());
            System.out.println("Registered: " + athlete.identify() + " and " + operator.identify());
            System.out.println("Reloaded highlight: " + reloaded.describe());
            System.out.println("Archive requested by athlete, filtered by court 1:");
            repository.findByCourt(1).forEach(item -> System.out.println(" - " + item.describe()));

            try {
                inactiveCourt.triggerCapture();
            } catch (NoActiveCameraException exception) {
                System.out.println("Expected error: " + exception.getMessage());
            }
        } catch (RepositoryException exception) {
            System.err.println("Archive error: " + exception.getMessage());
        }
    }
}
