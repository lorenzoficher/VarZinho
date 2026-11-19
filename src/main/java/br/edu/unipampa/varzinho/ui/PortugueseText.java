package br.edu.unipampa.varzinho.ui;

/** Translates model messages and descriptions only at the interface boundary. */
final class PortugueseText {
    private PortugueseText() { }

    static String cameraDescription(String description) {
        return description
                .replace(" · fixed · ", " · fixa · ")
                .replace(" · stream · ", " · transmissão · ")
                .replace(" · ACTIVE", " · ATIVA")
                .replace(" · INACTIVE", " · PARADA")
                .replace(" · MAINTENANCE", " · MANUTENÇÃO")
                .replace(" · pan ", " · panorâmica ")
                .replace(" tilt ", " inclinação ");
    }

    static String message(String message) {
        if (message == null || message.isBlank()) return "Não foi possível concluir a operação.";
        if (message.matches("line [0-9]+ of the archive cannot be read: .*")) {
            return "Não foi possível ler a linha " + message.split(" ", 3)[1] + " do acervo.";
        }
        if (message.matches("(pan|tilt|zoom) runs from -?[0-9]+ to -?[0-9]+, not -?[0-9]+")) {
            String axis = message.startsWith("pan") ? "A panorâmica" :
                    message.startsWith("tilt") ? "A inclinação" : "O zoom";
            return message.replaceFirst("^[a-z]+ runs from ", axis + " deve estar entre ")
                    .replace(" to ", " e ").replace(", not ", ". Valor informado: ") + ".";
        }
        String translated = message
                .replace("ffmpeg is not installed", "O FFmpeg não está instalado.")
                .replace(" is under maintenance and cannot record", " está em manutenção e não pode gravar")
                .replace(" is under maintenance and is not recording", " está em manutenção e já está parada")
                .replace(" is already installed on court ", " já está instalada na quadra ")
                .replace(" is already on court ", " já está na quadra ")
                .replace("nothing has been recorded yet", "nenhuma imagem foi gravada ainda")
                .replace("there is no active camera on court ", "não há câmera ativa na quadra ")
                .replace(" is not recording", " não está gravando")
                .replace("court number is already registered", "o número da quadra já está cadastrado")
                .replace("a court number must be positive", "o número da quadra deve ser positivo")
                .replace("a camera needs an identifier", "a câmera precisa de um identificador")
                .replace("a camera needs a model", "a câmera precisa de um modelo")
                .replace("an operator needs a name", "o operador precisa de um nome")
                .replace("an operator needs a username", "o operador precisa de um usuário")
                .replace("a username cannot contain whitespace", "o usuário não pode conter espaços")
                .replace("a password needs at least 8 characters", "a senha precisa ter pelo menos 8 caracteres")
                .replace("a password needs an uppercase letter", "a senha precisa de uma letra maiúscula")
                .replace("a password needs a lowercase letter", "a senha precisa de uma letra minúscula")
                .replace("a password needs a digit", "a senha precisa de um número")
                .replace("a password needs a symbol", "a senha precisa de um símbolo")
                .replace("the credentials are not valid", "usuário ou senha inválidos")
                .replace("cannot be aimed", "não pode ser apontada")
                .replace("the camera is not installed on court ", "a câmera não está instalada na quadra ")
                .replace("there is no camera to move", "não há câmera para mover");
        if (translated.equals(message)) return "Não foi possível concluir a operação.";
        return translated.replaceFirst("^camera ", "a câmera ");
    }
}
