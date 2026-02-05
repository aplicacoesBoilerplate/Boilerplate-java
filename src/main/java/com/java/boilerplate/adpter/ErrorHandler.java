package com.java.boilerplate.adpter;

import com.java.boilerplate.exception.IErrors;
import com.java.boilerplate.model.ErrorsPersistidos;
import com.java.boilerplate.repository.ErrorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@RestControllerAdvice
public class ErrorHandler {

    private final ErrorRepository errorRepository;

    public ErrorHandler(ErrorRepository errorRepository) {
        this.errorRepository = errorRepository;
    }

    //    Exception genérica para interceptar todas as exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorsPersistidos> handlerException(Exception e) {
        ErrorsPersistidos errorPersistido = new ErrorsPersistidos();
        StackTraceElement stackElement = e.getStackTrace()[0]; // primeiro elemento da stack
        Map<String, Object> traceInfo = Map.of(
                "fileName", stackElement.getFileName(),
                "className", stackElement.getClassName(),
                "methodName", stackElement.getMethodName(),
                "lineNumber", stackElement.getLineNumber()
        );

        errorPersistido.setArquivoError(stackElement.getFileName());
        errorPersistido.setClasseError(stackElement.getClassName());
        errorPersistido.setMetodoError(stackElement.getMethodName());
        errorPersistido.setLinhaError(stackElement.getLineNumber());
        errorPersistido.setHoraError(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")));

        if (e instanceof IErrors errors) {
            errorPersistido.setErro(errors.getErro());
            errorPersistido.setIdSaida(0);
            errorPersistido.setStatusCodeError(errors.getStatusCode());

            errorRepository.persistirError(errorPersistido);

            ErrorsPersistidos errorResponse = new ErrorsPersistidos();
            errorResponse.setErro(errors.getErro());
            errorResponse.setUsuario(usuario);
            errorResponse.setTrace(traceInfo);
            errorResponse.setHoraErro(errors.getHoraErro());
            errorResponse.setStatusCode(errors.getStatusCode());

            return new ResponseEntity<>(errorResponse, HttpStatus.valueOf(errors.getStatusCode()));
        }
        if (e instanceof SaidaErrorInterface saidaErrorInterface) {
            UsuarioTokenDTO usuario = saidaErrorInterface.getUsuario();

            errorPersistido.setErro(saidaErrorInterface.getErro());
            errorPersistido.setIdUsuario(usuario != null ? usuario.idUsuario() : null);
            errorPersistido.setNomeResponsavel(usuario != null ? usuario.nome() : null);
            errorPersistido.setEmailResponsavel(usuario != null ? usuario.email() : null);
            errorPersistido.setPermissaoResponsavel(usuario != null ? usuario.permissao() : null);
            errorPersistido.setIdUsuario(saidaErrorInterface.getUsuario().idUsuario());
            errorPersistido.setIdSaida(saidaErrorInterface.getSaida());
            errorPersistido.setStatusCodeError(saidaErrorInterface.getStatusCode());

            errorRepository.persistirError(errorPersistido);

            ErrorsPersistidos errorResponse = new ErrorsPersistidos();
            errorResponse.setErro(saidaErrorInterface.getErro());
            errorResponse.setUsuario(usuario);
            errorResponse.setSaida(saidaErrorInterface.getSaida());
            errorResponse.setTrace(traceInfo);
            errorResponse.setHoraErro(saidaErrorInterface.getHoraErro());
            errorResponse.setStatusCode(saidaErrorInterface.getStatusCode());
            return new ResponseEntity<>(errorResponse, HttpStatus.valueOf(saidaErrorInterface.getStatusCode()));
        }

        // Tratamento genérico caso não seja tratável
        ErrorsPersistidos defaultError = new ErrorsPersistidos(
                "Erro interno não tratado",
                null,
                0,
                traceInfo,
                dataAtualLocalDateTime(),
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        return new ResponseEntity<>(defaultError, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
