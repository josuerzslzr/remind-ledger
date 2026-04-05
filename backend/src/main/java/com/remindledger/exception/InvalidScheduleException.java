package com.remindledger.exception;

public class InvalidScheduleException extends RuntimeException {

    /**
     * Creates a new InvalidScheduleException with the specified detail message.
     *
     * @param message the detail message describing why the schedule is invalid
     */
    public InvalidScheduleException(String message) {
        super(message);
    }
}
