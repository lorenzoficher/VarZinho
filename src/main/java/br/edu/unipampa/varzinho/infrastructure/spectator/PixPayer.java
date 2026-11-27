package br.edu.unipampa.varzinho.infrastructure.spectator;

record PixPayer(String email, String cpf) {
    PixPayer {
        if (!email.matches("[A-Za-z0-9._%+\\-]{1,64}@[A-Za-z0-9.\\-]{1,190}\\.[A-Za-z]{2,63}")
                || !cpf.matches("[0-9]{11}") || cpf.chars().distinct().count() == 1
                || digit(cpf, 9) != cpf.charAt(9) - '0' || digit(cpf, 10) != cpf.charAt(10) - '0')
            throw new IllegalArgumentException("invalid Pix payer");
    }

    private static int digit(String cpf, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) sum += (cpf.charAt(i) - '0') * (length + 1 - i);
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    @Override public String toString() { return "PixPayer[redacted]"; }
}
