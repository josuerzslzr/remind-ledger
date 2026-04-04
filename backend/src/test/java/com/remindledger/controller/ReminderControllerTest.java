package com.remindledger.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.remindledger.dto.ReminderRequest;
import com.remindledger.dto.ReminderResponse;
import com.remindledger.exception.GlobalExceptionHandler;
import com.remindledger.exception.InvalidScheduleException;
import com.remindledger.exception.ReminderNotFoundException;
import com.remindledger.model.Channel;
import com.remindledger.model.ScheduleType;
import com.remindledger.service.ReminderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReminderController.class)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://test.example.com"
})
class ReminderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReminderService reminderService;

    @MockitoBean
    @SuppressWarnings("unused")
    private JwtDecoder jwtDecoder;

    private RequestPostProcessor testJwt;

    @BeforeEach
    void setUp() {
        testJwt = jwt().jwt(j -> j.subject("test-sub").claim("email", "test@example.com"));
    }

    @Test
    void createReminder_validRequest_returns201() throws Exception {
        var response = new ReminderResponse(
                UUID.randomUUID(), "Call dentist", ScheduleType.ONCE,
                List.of(LocalTime.of(15, 0)),
                LocalDate.of(2026, 4, 15),
                null, null,
                List.of(Channel.WEB_PUSH),
                null, Instant.now(), Instant.now());

        when(reminderService.create(any(), any())).thenReturn(response);

        var request = new ReminderRequest(
                "Call dentist", ScheduleType.ONCE,
                List.of(LocalTime.of(15, 0)),
                LocalDate.of(2026, 4, 15),
                null, null,
                List.of(Channel.WEB_PUSH), null);

        mockMvc.perform(post("/api/reminders")
                        .with(testJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Call dentist"))
                .andExpect(jsonPath("$.scheduleType").value("ONCE"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void createReminder_missingName_returns400() throws Exception {
        String body = """
                {
                  "scheduleType": "DAILY",
                  "times": ["09:00"],
                  "channels": ["WEB_PUSH"]
                }
                """;

        mockMvc.perform(post("/api/reminders")
                        .with(testJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void createReminder_missingChannels_returns400() throws Exception {
        String body = """
                {
                  "name": "Test",
                  "scheduleType": "DAILY",
                  "times": ["09:00"]
                }
                """;

        mockMvc.perform(post("/api/reminders")
                        .with(testJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.channels").exists());
    }

    @Test
    void createReminder_invalidSchedule_returns422() throws Exception {
        when(reminderService.create(any(), any()))
                .thenThrow(new InvalidScheduleException("date is required for ONCE schedule"));

        var request = new ReminderRequest(
                "Test", ScheduleType.ONCE,
                List.of(LocalTime.of(9, 0)),
                null, null, null,
                List.of(Channel.WEB_PUSH), null);

        mockMvc.perform(post("/api/reminders")
                        .with(testJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("date is required for ONCE schedule"));
    }

    @Test
    void createReminder_noAuth_rejectsRequest() throws Exception {
        String body = """
                {
                  "name": "Test",
                  "scheduleType": "DAILY",
                  "times": ["09:00"],
                  "channels": ["WEB_PUSH"]
                }
                """;

        mockMvc.perform(post("/api/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void listReminders_returnsUserReminders() throws Exception {
        var r1 = buildResponse(UUID.randomUUID(), "Vitamins");
        var r2 = buildResponse(UUID.randomUUID(), "Pay rent");
        when(reminderService.listForUser(any())).thenReturn(List.of(r1, r2));

        mockMvc.perform(get("/api/reminders").with(testJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Vitamins"))
                .andExpect(jsonPath("$[1].name").value("Pay rent"));
    }

    @Test
    void listReminders_noAuth_rejectsRequest() throws Exception {
        mockMvc.perform(get("/api/reminders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getReminder_existingId_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(reminderService.getById(eq(id), any())).thenReturn(buildResponse(id, "Dentist"));

        mockMvc.perform(get("/api/reminders/{id}", id).with(testJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Dentist"));
    }

    @Test
    void getReminder_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(reminderService.getById(eq(id), any())).thenThrow(new ReminderNotFoundException(id));

        mockMvc.perform(get("/api/reminders/{id}", id).with(testJwt))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void updateReminder_validRequest_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        var request = new ReminderRequest(
                "Updated", ScheduleType.DAILY,
                List.of(LocalTime.of(9, 0)),
                null, null, null,
                List.of(Channel.IN_APP), null);
        when(reminderService.update(eq(id), any(), any())).thenReturn(buildResponse(id, "Updated"));

        mockMvc.perform(put("/api/reminders/{id}", id)
                        .with(testJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    void updateReminder_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        var request = new ReminderRequest(
                "Updated", ScheduleType.DAILY,
                List.of(LocalTime.of(9, 0)),
                null, null, null,
                List.of(Channel.IN_APP), null);
        when(reminderService.update(eq(id), any(), any())).thenThrow(new ReminderNotFoundException(id));

        mockMvc.perform(put("/api/reminders/{id}", id)
                        .with(testJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReminder_existingId_returns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/reminders/{id}", id).with(testJwt))
                .andExpect(status().isNoContent());

        verify(reminderService).delete(eq(id), any());
    }

    @Test
    void deleteReminder_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ReminderNotFoundException(id)).when(reminderService).delete(eq(id), any());

        mockMvc.perform(delete("/api/reminders/{id}", id).with(testJwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReminder_noAuth_rejectsRequest() throws Exception {
        mockMvc.perform(delete("/api/reminders/{id}", UUID.randomUUID()))
                .andExpect(status().isForbidden());

        verify(reminderService, never()).delete(any(), any());
    }

    @Test
    void updateReminder_noAuth_rejectsRequest() throws Exception {
        String body = """
                {
                  "name": "Updated",
                  "scheduleType": "DAILY",
                  "times": ["09:00"],
                  "channels": ["IN_APP"]
                }
                """;

        mockMvc.perform(put("/api/reminders/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        verify(reminderService, never()).update(any(), any(), any());
    }

    private ReminderResponse buildResponse(UUID id, String name) {
        return new ReminderResponse(
                id, name, ScheduleType.DAILY,
                List.of(LocalTime.of(9, 0)),
                null, null, null,
                List.of(Channel.IN_APP),
                null, Instant.now(), Instant.now());
    }
}