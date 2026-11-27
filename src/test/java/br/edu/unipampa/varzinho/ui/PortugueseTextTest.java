package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.CameraView;
import br.edu.unipampa.varzinho.application.ManageGym;
import br.edu.unipampa.varzinho.domain.capture.FixedCamera;
import br.edu.unipampa.varzinho.domain.capture.PtzCamera;
import br.edu.unipampa.varzinho.domain.capture.StreamCamera;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryGymRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortugueseTextTest {
    @Test
    void distinguishesAMissingRecorderFromAStreamWithoutFootage() {
        var missing = new java.io.UncheckedIOException(new java.io.FileNotFoundException("ffmpeg"));
        assertTrue(PortugueseText.phoneConnectionFailure(missing).contains("FFmpeg não encontrado"));
        assertTrue(PortugueseText.phoneConnectionFailure(new IllegalStateException("no completed video segment"))
                .contains("primeiro trecho"));
        assertTrue(PortugueseText.phoneConnectionFailure(new IllegalStateException("recorder exited before footage"))
                .contains("buffer/recorder.log"));
    }
    @Test
    void reportsClipAssemblyFailuresUsingTheProductionMessageFormats() {
        assertEquals("O FFmpeg não conseguiu gravar o clipe.", PortugueseText.message(
                "ffmpeg could not write clips/camera-1.mp4: Invalid data\nConversion failed"));
        assertEquals("O FFmpeg demorou demais para gravar o clipe.", PortugueseText.message(
                "ffmpeg took longer than 5000 ms to write clips/camera-1.mp4"));
        assertEquals("Não foi possível gravar o arquivo do clipe.", PortugueseText.message(
                "could not write clips/camera-1.mp4"));
        assertEquals("A gravação do clipe foi interrompida.", PortugueseText.message(
                "interrupted while writing clips/camera-1.mp4"));
    }

    @Test
    void describesAllCameraKindsFromTheirPropertiesAndLeavesIdentifiersIntact() {
        Gym gym = new Gym("Arena", "Rua A");
        gym.addCourt(new Court(1));
        FixedCamera fixed = new FixedCamera("CAM · fixed · ACTIVE", "Dome", Resolution.HD, 30, 45);
        fixed.startRecording();
        gym.installCamera(1, fixed);
        PtzCamera ptz = new PtzCamera("CAM-2", "PTZ", Resolution.FULL_HD, 30);
        ptz.moveTo(125, -12, 3);
        gym.installCamera(1, ptz);
        StreamCamera phone = new StreamCamera("CAM-3", "Celular", Resolution.HD, 30,
                "http://phone/video", (frames, target) -> { });
        phone.sendToMaintenance();
        gym.installCamera(1, phone);
        List<CameraView> cameras = new ManageGym(gym, new InMemoryGymRepository()).court(1).getCameras();

        assertEquals(List.of("CAM · fixed · ACTIVE · Fixa · HD (1280x720) · Ativa",
                        "CAM-2 · PTZ · Full HD (1920x1080) · Parada · panorâmica 125 inclinação -12 zoom 3",
                        "CAM-3 · Transmissão · HD (1280x720) · Em manutenção"),
                cameras.stream().map(PortugueseText::cameraDescription).toList());
    }

    @Test
    void translatesTheRefusalsOfACourtsLabelsAsTheDomainWordsThem() {
        Court court = new Court(1);
        String tooLong = "x".repeat(Court.MAX_LABEL_LENGTH + 1);

        assertEquals("O nome de exibição da quadra pode ter no máximo " + Court.MAX_LABEL_LENGTH + " caracteres.",
                PortugueseText.message(refusal(() -> court.rename(tooLong))));
        assertEquals("A modalidade da quadra pode ter no máximo " + Court.MAX_LABEL_LENGTH + " caracteres.",
                PortugueseText.message(refusal(() -> court.assignSport(tooLong))));
        assertEquals("O nome de exibição da quadra deve caber em uma linha.",
                PortugueseText.message(refusal(() -> court.rename("a\nb"))));
        assertEquals("A modalidade da quadra deve caber em uma linha.",
                PortugueseText.message(refusal(() -> court.assignSport("a\nb"))));
    }

    private static String refusal(Runnable change) {
        return org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, change::run).getMessage();
    }

    @Test
    void namesTheCameraAndCourtInLookupRefusals() {
        assertEquals("A câmera CAM-3 não está instalada na quadra 2.", PortugueseText.message(
                "camera CAM-3 is not installed on court 2"));
        assertEquals("A câmera CAM-3 não está instalada neste ginásio.", PortugueseText.message(
                "camera CAM-3 is not installed in this gym"));
        assertEquals("A quadra 2 não está cadastrada.", PortugueseText.message("court 2 is not registered"));
        assertEquals("A quadra 2 ainda tem câmeras. Remova ou mova as câmeras antes de removê-la.",
                PortugueseText.message("court 2 still holds cameras"));
        assertEquals("A quadra 2 é a única da academia e não pode ser removida.",
                PortugueseText.message("court 2 is the only court"));
        assertEquals("O primeiro operador já foi cadastrado.", PortugueseText.message(
                "the first operator has already been created"));
    }

    @Test
    void logsTheOriginalWhenARefusalHasNoTranslation() {
        String original = "the adapter returned an unknown result";
        Logger logger = Logger.getLogger(PortugueseText.class.getName());
        List<String> diagnostics = new ArrayList<>();
        Handler observer = new Handler() {
            @Override public void publish(LogRecord record) { diagnostics.add(record.getMessage()); }
            @Override public void flush() { }
            @Override public void close() { }
        };
        logger.addHandler(observer);
        try {
            assertEquals("Não foi possível concluir a operação.", PortugueseText.message(original));
            assertTrue(diagnostics.stream().anyMatch(message -> message.contains(original)));
        } finally {
            logger.removeHandler(observer);
        }
    }
}
