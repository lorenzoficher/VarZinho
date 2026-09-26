package br.edu.unipampa.varzinho.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.exception.RepositoryException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CsvHighlightRepositoryDamageTest {

    private static final String HEADER =
            "id,capturedAt,courtNumber,cameraId,clipPath,durationSeconds,resolution,sizeMb";
    private static final String GOOD_1 = "h-001,2026-09-12T20:14:33Z,3,cam-a1,/clips/h-001.mp4,30,FULL_HD,42.5";
    private static final String GOOD_2 = "h-003,2026-09-12T20:41:02Z,3,cam-a1,/clips/h-003.mp4,30,FULL_HD,41.8";

    private static final String OTHER_COURT = "h-004,2026-09-12T20:50:00Z,5,cam-b1,/clips/h-004.mp4,30,FULL_HD,40.0";

    @TempDir
    private Path archive;

    @Test
    void aMalformedLineRaisesACorruptedRecordException() throws IOException {
        HighlightRepository repository = repositoryOver(HEADER, GOOD_1, "h-002,not-a-date,3", GOOD_2);

        assertThrows(CorruptedRecordException.class, repository::findAll);
    }

    @Test
    void theExceptionNamesTheOffendingLineNumber() throws IOException {
        HighlightRepository repository = repositoryOver(HEADER, GOOD_1, "h-002,not-a-date,3", GOOD_2);

        CorruptedRecordException damage =
                assertThrows(CorruptedRecordException.class, repository::findAll);

        assertEquals(3, damage.getLineNumber());
        assertTrue(damage.getMessage().contains("line 3"));
    }

    @Test
    void validRecordsBeforeAndAfterTheMalformedLineAreStillLoaded() throws IOException {
        HighlightRepository repository = repositoryOver(HEADER, GOOD_1, "h-002,not-a-date,3", GOOD_2);

        CorruptedRecordException damage =
                assertThrows(CorruptedRecordException.class, repository::findAll);

        List<String> recovered = damage.getRecovered().stream()
                .map(Highlight::getId).collect(Collectors.toList());
        assertEquals(List.of("h-001", "h-003"), recovered);
    }

    @Test
    void recoveredHighlightsOfACourtSearchBelongToThatCourt() throws IOException {
        HighlightRepository repository =
                repositoryOver(HEADER, GOOD_1, OTHER_COURT, "h-002,not-a-date,3", GOOD_2);

        CorruptedRecordException damage =
                assertThrows(CorruptedRecordException.class, () -> repository.findByCourt(5));

        assertEquals(List.of("h-004"), damage.getRecovered().stream()
                .map(Highlight::getId).collect(Collectors.toList()));
    }

    @Test
    void recoveredHighlightsOfAnIdSearchAreOnlyThatHighlight() throws IOException {
        HighlightRepository repository =
                repositoryOver(HEADER, GOOD_1, OTHER_COURT, "h-002,not-a-date,3", GOOD_2);

        CorruptedRecordException damage =
                assertThrows(CorruptedRecordException.class, () -> repository.findById("h-004"));

        assertEquals(List.of("h-004"), damage.getRecovered().stream()
                .map(Highlight::getId).collect(Collectors.toList()));
    }

    @Test
    void aRecordThatBreaksTheHighlightRulesCountsAsMalformed() throws IOException {
        String zeroLengthClip = "h-002,2026-09-12T20:14:33Z,3,cam-a1,/clips/h-002.mp4,0,FULL_HD,1.0";
        HighlightRepository repository = repositoryOver(HEADER, zeroLengthClip);

        assertThrows(CorruptedRecordException.class, repository::findAll);
    }

    @Test
    void aFileWithOnlyAHeaderLoadsAsEmpty() throws IOException, RepositoryException {
        HighlightRepository repository = repositoryOver(HEADER);

        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void anEmptyFileLoadsAsEmpty() throws IOException, RepositoryException {
        HighlightRepository repository = repositoryOver();

        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void aBlankLineIsSkippedWithoutError() throws IOException, RepositoryException {
        HighlightRepository repository = repositoryOver(HEADER, GOOD_1, "", "   ", GOOD_2);

        assertEquals(2, repository.findAll().size());
    }

    @Test
    void savingIntoADamagedArchiveStoresTheHighlightAndKeepsTheDamagedLine() throws Exception {
        Path file = archive.resolve("highlights.csv");
        Files.write(file, List.of(HEADER, GOOD_1, "garbage", GOOD_2));
        HighlightRepository repository = new CsvHighlightRepository(file);

        repository.save(HighlightRepositoryContractTest.highlight("h-009", 3));

        List<String> lines = Files.readAllLines(file);
        assertEquals(List.of(HEADER, GOOD_1, "garbage", GOOD_2), lines.subList(0, 4));
        assertEquals(5, lines.size());
        assertTrue(lines.get(4).startsWith("h-009,"));
    }

    @Test
    void theDamageIsStillReportedOnTheNextReadAfterASave() throws Exception {
        Path file = archive.resolve("highlights.csv");
        Files.write(file, List.of(HEADER, GOOD_1, "garbage"));
        HighlightRepository repository = new CsvHighlightRepository(file);
        repository.save(HighlightRepositoryContractTest.highlight("h-009", 3));

        CorruptedRecordException damage =
                assertThrows(CorruptedRecordException.class, repository::findAll);

        assertEquals(3, damage.getLineNumber());
        assertEquals(List.of("h-001", "h-009"), damage.getRecovered().stream()
                .map(Highlight::getId).collect(Collectors.toList()));
    }

    @Test
    void savingAnExistingIdInADamagedArchiveReplacesThatLine() throws Exception {
        Path file = archive.resolve("highlights.csv");
        Files.write(file, List.of(HEADER, "garbage", GOOD_1));
        HighlightRepository repository = new CsvHighlightRepository(file);

        repository.save(HighlightRepositoryContractTest.highlight("h-001", 7));

        List<String> lines = Files.readAllLines(file);
        assertEquals(3, lines.size());
        assertEquals("garbage", lines.get(1));
        assertTrue(lines.get(2).startsWith("h-001,"));
        assertTrue(lines.get(2).contains(",7,"));
    }

    private HighlightRepository repositoryOver(String... lines) throws IOException {
        Path file = archive.resolve("highlights.csv");
        Files.write(file, List.of(lines));
        return new CsvHighlightRepository(file);
    }
}
