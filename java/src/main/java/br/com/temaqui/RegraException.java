package br.com.temaqui;

import org.springframework.http.HttpStatus;

public class RegraException extends RuntimeException {
    private final HttpStatus status;

    public RegraException(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }

    public static RegraException ausente(String recurso) {
        return new RegraException(HttpStatus.NOT_FOUND, recurso + " inexistente.");
    }

    public static RegraException conflito(String mensagem) {
        return new RegraException(HttpStatus.CONFLICT, mensagem);
    }
}
