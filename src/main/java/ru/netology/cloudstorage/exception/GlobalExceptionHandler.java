package ru.netology.cloudstorage.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import ru.netology.cloudstorage.dto.ErrorResponse;
import ru.netology.cloudstorage.util.ErrorCodes;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(BadRequestException ex) {
        int errorCode = ErrorCodes.INVALID_INPUT_DATA;

        if (ex.getMessage().contains("credentials")) {
            errorCode = ErrorCodes.BAD_CREDENTIALS;
        } else if (ex.getMessage().contains("exists")) {
            errorCode = ErrorCodes.FILE_ALREADY_EXISTS;
        }

        ErrorResponse error = new ErrorResponse(ex.getMessage(), errorCode);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(UnauthorizedException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), ErrorCodes.UNAUTHORIZED_ACCESS);
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler({InternalServerErrorException.class, Exception.class})
    public ResponseEntity<ErrorResponse> handleInternalServerError(Exception ex) {
        logger.error("Internal Server Error occurred:", ex);

        int errorCode = ErrorCodes.SERVER_ERROR;
        String message = "Internal server error";

        if (ex instanceof InternalServerErrorException) {
            message = ex.getMessage();
            if (message.contains("file") || message.contains("disk")) {
                errorCode = ErrorCodes.FILE_SYSTEM_ERROR;
            }
        }

        ErrorResponse error = new ErrorResponse(message, errorCode);
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
