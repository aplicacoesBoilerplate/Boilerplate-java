package com.java.boilerplate.exception;

import com.java.boilerplate.dto.DTOError;
import com.java.boilerplate.model.LogErrors;
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<DTOError> handlerException(Exception e) {
        LocalDateTime errorDateTime = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));

        LogErrors logError = new LogErrors();
        StackTraceElement stackElement = e.getStackTrace()[0];

        assert stackElement.getFileName() != null;
        Map<String, Object> traceInfo = Map.of(
                "fileName", stackElement.getFileName(),
                "className", stackElement.getClassName(),
                "methodName", stackElement.getMethodName(),
                "lineNumber", stackElement.getLineNumber()
        );

        logError.setErrorFile(stackElement.getFileName());
        logError.setErrorClass(stackElement.getClassName());
        logError.setErrorMethod(stackElement.getMethodName());
        logError.setErrorLine(stackElement.getLineNumber());
        logError.setErrorDateTime(errorDateTime);

        if (e instanceof IErrors errors) {
            logError.setErrorMessage(errors.getErro());
            logError.setErrorStatusCode(errors.getStatusCode());

            errorRepository.save(logError);

            HttpStatus castStatusCode = HttpStatus.valueOf(errors.getStatusCode());

            DTOError errorResponse = new DTOError(
                    errors.getErro(),
                    traceInfo,
                    errors.getHoraErro(),
                    castStatusCode
            );

            return new ResponseEntity<>(errorResponse, castStatusCode);
        }

        // Tratamento genérico caso não seja tratável
        DTOError defaultError = new DTOError(
                "Unhandled internal error",
                traceInfo,
                errorDateTime,
                HttpStatus.INTERNAL_SERVER_ERROR
        );
        return new ResponseEntity<>(defaultError, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
