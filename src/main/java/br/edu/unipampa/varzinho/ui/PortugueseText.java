package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.CameraKind;
import br.edu.unipampa.varzinho.application.CameraView;

import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Keeps operator-facing language at the interface boundary. */
final class PortugueseText {
    private static final Logger LOGGER = Logger.getLogger(PortugueseText.class.getName());
    private static final String GENERIC = "Não foi possível concluir a operação.";

    private record MessagePattern(Pattern english, String portuguese) {
        MessagePattern(String english, String portuguese) {
            this(Pattern.compile(english, Pattern.DOTALL), portuguese);
        }
    }

    // Match the whole diagnostic so identifiers and paths are never translated as prose.
    private static final List<MessagePattern> PATTERNS = List.of(
            new MessagePattern("ffmpeg could not write .+", "O FFmpeg não conseguiu gravar o clipe."),
            new MessagePattern("ffmpeg took longer than [0-9]+ ms to write .+", "O FFmpeg demorou demais para gravar o clipe."),
            new MessagePattern("could not write .+", "Não foi possível gravar o arquivo do clipe."),
            new MessagePattern("interrupted while writing .+", "A gravação do clipe foi interrompida."),
            new MessagePattern("line ([0-9]+) of the archive cannot be read: .*", "Não foi possível ler a linha $1 do acervo."),
            new MessagePattern("pan runs from (-?[0-9]+) to (-?[0-9]+), not (-?[0-9]+)", "A panorâmica deve estar entre $1 e $2. Valor informado: $3."),
            new MessagePattern("tilt runs from (-?[0-9]+) to (-?[0-9]+), not (-?[0-9]+)", "A inclinação deve estar entre $1 e $2. Valor informado: $3."),
            new MessagePattern("zoom runs from (-?[0-9]+) to (-?[0-9]+), not (-?[0-9]+)", "O zoom deve estar entre $1 e $2. Valor informado: $3."),
            new MessagePattern("camera (.+) is under maintenance and cannot record", "A câmera $1 está em manutenção e não pode gravar."),
            new MessagePattern("camera (.+) is under maintenance and is not recording", "A câmera $1 está em manutenção e já está parada."),
            new MessagePattern("camera (.+) is already installed on court (-?[0-9]+)", "A câmera $1 já está instalada na quadra $2."),
            new MessagePattern("camera (.+) is already on court (-?[0-9]+)", "A câmera $1 já está na quadra $2."),
            new MessagePattern("camera (.+) is not installed on court (-?[0-9]+)", "A câmera $1 não está instalada na quadra $2."),
            new MessagePattern("camera (.+) is not installed in this gym", "A câmera $1 não está instalada neste ginásio."),
            new MessagePattern("camera (.+) is not recording", "A câmera $1 não está gravando."),
            new MessagePattern("camera (.+) cannot be aimed", "A câmera $1 não pode ser apontada."),
            new MessagePattern("the camera is not installed on court (-?[0-9]+)", "A câmera não está instalada na quadra $1."),
            new MessagePattern("court (-?[0-9]+) is not registered", "A quadra $1 não está cadastrada."),
            new MessagePattern("Court ([0-9]+) has no active camera[.]", "Não há câmera ativa na quadra $1."),
            new MessagePattern("a window of ([0-9]+)s was asked of the ([0-9]+)s recorded", "A captura requer $1 s, mas apenas $2 s foram gravados.")
    );

    private PortugueseText() { }

    static String cameraKind(CameraKind kind) {
        return switch (kind) {
            case FIXED -> "Fixa";
            case PTZ -> "PTZ";
            case STREAM -> "Transmissão";
        };
    }

    static String cameraDescription(CameraView camera) {
        String status = switch (camera.getStatus()) {
            case ACTIVE -> "Ativa";
            case INACTIVE -> "Parada";
            case MAINTENANCE -> "Em manutenção";
        };
        String line = String.join(" · ", camera.getId(), cameraKind(camera.getKind()), camera.getResolution().label(), status);
        if (camera.getKind() != CameraKind.PTZ) return line;
        return line + " · panorâmica " + camera.getPan().getAsInt()
                + " inclinação " + camera.getTilt().getAsInt() + " zoom " + camera.getZoom().getAsInt();
    }

    static String message(String message) {
        if (message == null || message.isBlank()) return untranslated(message);
        return switch (message) {
            case "nothing has been recorded yet" -> "Nenhuma imagem foi gravada ainda.";
            case "court number is already registered" -> "O número da quadra já está cadastrado.";
            case "a court number must be positive" -> "O número da quadra deve ser positivo.";
            case "a camera needs an identifier" -> "A câmera precisa de um identificador.";
            case "a camera needs a model" -> "A câmera precisa de um modelo.";
            case "an operator needs a name" -> "O operador precisa de um nome.";
            case "an operator needs a username" -> "O operador precisa de um usuário.";
            case "a username cannot contain whitespace" -> "O usuário não pode conter espaços.";
            case "a password needs at least 8 characters" -> "A senha precisa ter pelo menos 8 caracteres.";
            case "a password needs an uppercase letter" -> "A senha precisa de uma letra maiúscula.";
            case "a password needs a lowercase letter" -> "A senha precisa de uma letra minúscula.";
            case "a password needs a digit" -> "A senha precisa de um número.";
            case "a password needs a symbol" -> "A senha precisa de um símbolo.";
            case "username or password is incorrect" -> "Usuário ou senha inválidos.";
            case "the first operator has already been created" -> "O primeiro operador já foi cadastrado.";
            case "there is no camera to move" -> "Não há câmera para mover.";
            default -> translateDetails(message);
        };
    }

    private static String translateDetails(String message) {
        for (MessagePattern pattern : PATTERNS) {
            Matcher match = pattern.english().matcher(message);
            if (match.matches()) return match.replaceFirst(pattern.portuguese());
        }
        return untranslated(message);
    }

    private static String untranslated(String message) {
        LOGGER.warning(() -> "Untranslated operator message: " + message);
        return GENERIC;
    }
}
