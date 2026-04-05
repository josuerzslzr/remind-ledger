package com.remindledger.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Convert method-argument validation failures into a structured HTTP 400 ProblemDetail.
     *
     * Builds a ProblemDetail with status 400 (Bad Request), detail message "Validation failed",
     * and a `fieldErrors` property mapping each invalid field name to a list of its validation messages.
     *
     * @param ex the MethodArgumentNotValidException containing validation results
     * @return a ProblemDetail with status 400 and a `fieldErrors` property grouping messages by field
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Validation failed");
        Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> fieldErrors
                        .computeIfAbsent(e.getField(), k -> new ArrayList<>())
                        .add(e.getDefaultMessage()));
        pd.setProperty("fieldErrors", fieldErrors);
        return pd;
    }

    /**
     * Create a ProblemDetail response for an InvalidScheduleException with HTTP 422 and the exception message as the problem detail.
     *
     * @param ex the InvalidScheduleException whose message will be used as the problem detail
     * @return a ProblemDetail with status 422 Unprocessable Entity and the exception's message as the detail
     */
    @ExceptionHandler(InvalidScheduleException.class)
    public ProblemDetail handleInvalidSchedule(InvalidScheduleException ex) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    /**
     * Converts a ReminderNotFoundException into a ProblemDetail representing a 404 Not Found error.
     *
     * @param ex the exception whose message will be used as the ProblemDetail detail
     * @return a ProblemDetail with HTTP status 404 (Not Found) and the exception message as its detail
     */
    @ExceptionHandler(ReminderNotFoundException.class)
    public ProblemDetail handleReminderNotFound(ReminderNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
