package com.remindledger.exception;

import java.util.UUID;

public class ReminderNotFoundException extends RuntimeException {

    /**
     * Creates an unchecked exception indicating a reminder with the given identifier was not found.
     *
     * @param id the UUID of the missing reminder; included in the exception message
     */
    public ReminderNotFoundException(UUID id) {
        super("Reminder not found: " + id);
    }
}
