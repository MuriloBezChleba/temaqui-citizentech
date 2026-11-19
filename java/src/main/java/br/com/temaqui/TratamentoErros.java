package br.com.temaqui;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratamentoErros {
    @ExceptionHandler(RegraException.class)
    ProblemDetail regra(RegraException erro) {
        return ProblemDetail.forStatusAndDetail(erro.status(), erro.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validacao(MethodArgumentNotValidException erro) {
        var resposta = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Campos invalidos.");
        resposta.setProperty("campos", erro.getBindingResult().getFieldErrors().stream()
                .map(campo -> campo.getField()).distinct().toList());
        return resposta;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integridade() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Registro duplicado ou restricao de dados.");
    }

    @ExceptionHandler(DataAccessResourceFailureException.class)
    ProblemDetail indisponivel() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Banco temporariamente indisponivel.");
    }

    @ExceptionHandler(DataAccessException.class)
    ProblemDetail persistencia(DataAccessException erro) {
        org.slf4j.LoggerFactory.getLogger(TratamentoErros.class)
                .error("Falha de persistencia ({})", erro.getClass().getSimpleName());
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao acessar o banco.");
    }
}
