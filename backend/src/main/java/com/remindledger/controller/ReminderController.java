package com.remindledger.controller;

import com.remindledger.dto.ReminderRequest;
import com.remindledger.dto.ReminderResponse;
import com.remindledger.service.ReminderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reminders")
@Tag(name = "Reminders")
public class ReminderController {

    private final ReminderService reminderService;

    /**
     * Creates a ReminderController that routes reminder-related HTTP requests.
     *
     * @param reminderService service used to perform reminder operations for authenticated users
     */
    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    /**
     * Retrieve all reminders belonging to the authenticated user.
     *
     * @param auth the JWT authentication token of the requesting user used to scope which reminders are returned
     * @return a list of ReminderResponse representing the user's reminders
     */
    @GetMapping
    @Operation(summary = "List all reminders for the authenticated user")
    public List<ReminderResponse> list(JwtAuthenticationToken auth) {
        return reminderService.listForUser(auth);
    }

    /**
     * Retrieve a reminder by its UUID for the authenticated user.
     *
     * @param id   the UUID of the reminder to retrieve
     * @param auth the JWT authentication token of the requesting user used to scope access
     * @return     the reminder representation as a {@code ReminderResponse}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get a reminder by ID")
    public ReminderResponse get(@PathVariable UUID id, JwtAuthenticationToken auth) {
        return reminderService.getById(id, auth);
    }

    /**
     * Create a new reminder for the authenticated user.
     *
     * @param request the reminder payload to create
     * @param auth    authentication token for the current user used to scope the reminder
     * @return        the created reminder as a ReminderResponse
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new reminder")
    public ReminderResponse create(
            @Valid @RequestBody ReminderRequest request,
            JwtAuthenticationToken auth) {
        return reminderService.create(request, auth);
    }

    /**
     * Replace an existing reminder identified by `id` with the provided reminder data.
     *
     * @param id      the UUID of the reminder to replace
     * @param request the replacement reminder data
     * @param auth    the authentication token of the current user used to scope the operation
     * @return        the updated ReminderResponse for the replaced reminder
     */
    @PutMapping("/{id}")
    @Operation(summary = "Replace a reminder")
    public ReminderResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody ReminderRequest request,
            JwtAuthenticationToken auth) {
        return reminderService.update(id, request, auth);
    }

    /**
     * Deletes the reminder identified by the given UUID for the authenticated user.
     *
     * @param id the UUID of the reminder to delete
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a reminder")
    public void delete(@PathVariable UUID id, JwtAuthenticationToken auth) {
        reminderService.delete(id, auth);
    }
}
