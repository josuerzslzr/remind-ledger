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

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @GetMapping
    @Operation(summary = "List all reminders for the authenticated user")
    public List<ReminderResponse> list(JwtAuthenticationToken auth) {
        return reminderService.listForUser(auth);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a reminder by ID")
    public ReminderResponse get(@PathVariable UUID id, JwtAuthenticationToken auth) {
        return reminderService.getById(id, auth);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new reminder")
    public ReminderResponse create(
            @Valid @RequestBody ReminderRequest request,
            JwtAuthenticationToken auth) {
        return reminderService.create(request, auth);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a reminder")
    public ReminderResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody ReminderRequest request,
            JwtAuthenticationToken auth) {
        return reminderService.update(id, request, auth);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a reminder")
    public void delete(@PathVariable UUID id, JwtAuthenticationToken auth) {
        reminderService.delete(id, auth);
    }
}
