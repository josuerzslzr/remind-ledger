package com.remindledger.service;

import com.remindledger.dto.ReminderRequest;
import com.remindledger.dto.ReminderResponse;
import com.remindledger.exception.InvalidScheduleException;
import com.remindledger.exception.ReminderNotFoundException;
import com.remindledger.model.Channel;
import com.remindledger.model.Reminder;
import com.remindledger.model.User;
import com.remindledger.repository.ReminderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
public class ReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

    private final ReminderRepository reminderRepository;
    private final UserService userService;

    public ReminderService(ReminderRepository reminderRepository, UserService userService) {
        this.reminderRepository = reminderRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<ReminderResponse> listForUser(JwtAuthenticationToken auth) {
        User user = userService.getOrCreateUser(auth);
        return reminderRepository.findAllByUser(user).stream()
                .map(ReminderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReminderResponse getById(UUID id, JwtAuthenticationToken auth) {
        User user = userService.getOrCreateUser(auth);
        return reminderRepository.findByIdAndUser(id, user)
                .map(ReminderResponse::from)
                .orElseGet(() -> {
                    logUnauthorizedAccessIfExists(id, user);
                    throw new ReminderNotFoundException(id);
                });
    }

    @Transactional
    public ReminderResponse update(UUID id, ReminderRequest request, JwtAuthenticationToken auth) {
        User user = userService.getOrCreateUser(auth);
        Reminder reminder = reminderRepository.findByIdAndUser(id, user)
                .orElseGet(() -> {
                    logUnauthorizedAccessIfExists(id, user);
                    throw new ReminderNotFoundException(id);
                });
        validateScheduleFields(request);
        applyUpdate(reminder, request);
        return ReminderResponse.from(reminderRepository.save(reminder));
    }

    @Transactional
    public void delete(UUID id, JwtAuthenticationToken auth) {
        User user = userService.getOrCreateUser(auth);
        Reminder reminder = reminderRepository.findByIdAndUser(id, user)
                .orElseGet(() -> {
                    logUnauthorizedAccessIfExists(id, user);
                    throw new ReminderNotFoundException(id);
                });
        reminderRepository.delete(reminder);
    }

    @Transactional
    public ReminderResponse create(ReminderRequest request, JwtAuthenticationToken auth) {
        User user = userService.getOrCreateUser(auth);
        validateScheduleFields(request);

        Reminder reminder = toEntity(request, user);
        reminder = reminderRepository.save(reminder);
        return ReminderResponse.from(reminder);
    }

    private void validateScheduleFields(ReminderRequest req) {
        switch (req.scheduleType()) {
            case ONCE -> {
                requireNonNull(req.date(), "date is required for ONCE schedule");
                requireSingleTime(req);
                requireNull(req.daysOfWeek(), "daysOfWeek must be null for ONCE schedule");
                requireNull(req.dayOfMonth(), "dayOfMonth must be null for ONCE schedule");
            }
            case DAILY -> {
                requireNull(req.date(), "date must be null for DAILY schedule");
                requireNull(req.daysOfWeek(), "daysOfWeek must be null for DAILY schedule");
                requireNull(req.dayOfMonth(), "dayOfMonth must be null for DAILY schedule");
            }
            case WEEKLY -> {
                requireNonNull(req.daysOfWeek(), "daysOfWeek is required for WEEKLY schedule");
                if (req.daysOfWeek().isEmpty()) {
                    throw new InvalidScheduleException("daysOfWeek must not be empty for WEEKLY schedule");
                }
                requireNull(req.date(), "date must be null for WEEKLY schedule");
                requireNull(req.dayOfMonth(), "dayOfMonth must be null for WEEKLY schedule");
            }
            case MONTHLY -> {
                requireNonNull(req.dayOfMonth(), "dayOfMonth is required for MONTHLY schedule");
                requireNull(req.date(), "date must be null for MONTHLY schedule");
                requireNull(req.daysOfWeek(), "daysOfWeek must be null for MONTHLY schedule");
            }
        }
    }

    private void requireSingleTime(ReminderRequest req) {
        if (req.times().size() != 1) {
            throw new InvalidScheduleException("ONCE schedule requires exactly one time entry");
        }
    }

    private void requireNonNull(Object value, String message) {
        if (value == null) {
            throw new InvalidScheduleException(message);
        }
    }

    private void requireNull(Object value, String message) {
        if (value != null) {
            throw new InvalidScheduleException(message);
        }
    }

    private void logUnauthorizedAccessIfExists(UUID id, User requestingUser) {
        if (reminderRepository.existsById(id)) {
            log.warn("User {} attempted to access reminder {} owned by another user",
                    requestingUser.getId(), id);
        }
    }

    private void applyUpdate(Reminder reminder, ReminderRequest req) {
        reminder.setName(req.name());
        reminder.setScheduleType(req.scheduleType());
        reminder.setTimes(req.times().toArray(LocalTime[]::new));
        reminder.setDate(req.date());
        reminder.setDaysOfWeek(req.daysOfWeek() != null
                ? req.daysOfWeek().toArray(DayOfWeek[]::new)
                : null);
        reminder.setDayOfMonth(req.dayOfMonth());
        reminder.setChannels(req.channels().toArray(Channel[]::new));
        reminder.setValidUntil(req.validUntil());
    }

    private Reminder toEntity(ReminderRequest req, User user) {
        Reminder r = new Reminder();
        r.setUser(user);
        applyUpdate(r, req);
        return r;
    }
}
