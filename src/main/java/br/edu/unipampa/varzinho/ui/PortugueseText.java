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
                .replace(" tilt ", " inclinação ")
                .replace(" zoom ", " zoom ");
    }

    static String message(String message) {
        if (message == null) return null;
        return message
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
    }
}
