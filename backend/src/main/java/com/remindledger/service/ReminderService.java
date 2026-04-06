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

    /**
     * Create a ReminderService wired with the required persistence and user-resolution components.
     *
     * @param reminderRepository repository used to perform CRUD operations on reminders
     */
    public ReminderService(ReminderRepository reminderRepository) {
        this.reminderRepository = reminderRepository;
    }

    /**
     * Lists all reminders belonging to the authenticated user represented by the given JWT token.
     *
     * @param user the user entity already extracted 
     * @return a list of ReminderResponse objects representing the user's reminders
     */
    @Transactional(readOnly = true)
    public List<ReminderResponse> listForUser(User user) {
        return reminderRepository.findAllByUser(user).stream()
                .map(ReminderResponse::from)
                .toList();
    }

    /**
     * Fetches a reminder by its ID for the authenticated user.
     *
     * @param id   the UUID of the reminder to retrieve
     * @param user the user entity already extracted 
     * @return     the reminder converted to a {@code ReminderResponse}
     * @throws ReminderNotFoundException if no reminder with the given id exists for the authenticated user
     */
    @Transactional(readOnly = true)
    public ReminderResponse getById(UUID id, User user) {
        return reminderRepository.findByIdAndUser(id, user)
                .map(ReminderResponse::from)
                .orElseGet(() -> {
                    logUnauthorizedAccessIfExists(id, user);
                    throw new ReminderNotFoundException(id);
                });
    }

    /**
     * Update an existing reminder belonging to the authenticated user.
     *
     * @param id      the UUID of the reminder to update
     * @param request the new reminder fields to apply
     * @param auth    the caller's authentication token used to resolve the owning user
     * @return        the updated reminder as a {@code ReminderResponse}
     * @throws ReminderNotFoundException if no reminder with the given id exists for the authenticated user
     * @throws InvalidScheduleException  if the provided schedule fields in {@code request} violate validation rules
     */
    @Transactional
    public ReminderResponse update(UUID id, ReminderRequest request, User user) {
        Reminder reminder = reminderRepository.findByIdAndUser(id, user)
                .orElseGet(() -> {
                    logUnauthorizedAccessIfExists(id, user);
                    throw new ReminderNotFoundException(id);
                });
        validateScheduleFields(request);
        applyUpdate(reminder, request);
        return ReminderResponse.from(reminderRepository.save(reminder));
    }

    /**
     * Deletes the reminder identified by the given id belonging to the authenticated user.
     *
     * @param id   the UUID of the reminder to delete
     * @param auth the authentication token used to resolve the requesting user
     * @throws ReminderNotFoundException if no reminder with the given id exists for the authenticated user
     */
    @Transactional
    public void delete(UUID id, User user) {
        Reminder reminder = reminderRepository.findByIdAndUser(id, user)
                .orElseGet(() -> {
                    logUnauthorizedAccessIfExists(id, user);
                    throw new ReminderNotFoundException(id);
                });
        reminderRepository.delete(reminder);
    }

    /**
     * Creates a new reminder for the authenticated user.
     *
     * Validates schedule-related fields from the request, associates the new reminder with
     * the caller's user record resolved from the provided authentication token, persists it,
     * and returns a response representation.
     *
     * @param request the reminder data to create
     * @param auth    the caller's JWT authentication token used to resolve or create the User
     * @return        a ReminderResponse representing the newly created reminder
     */
    @Transactional
    public ReminderResponse create(ReminderRequest request, User user) {
        validateScheduleFields(request);
        Reminder reminder = toEntity(request, user);
        reminder = reminderRepository.save(reminder);
        return ReminderResponse.from(reminder);
    }

    /**
     * Validates schedule-related fields on the given ReminderRequest according to its schedule type.
     *
     * For ONCE: requires `date` and exactly one entry in `times`; `daysOfWeek` and `dayOfMonth` must be null.
     * For DAILY: `date`, `daysOfWeek`, and `dayOfMonth` must be null.
     * For WEEKLY: `daysOfWeek` must be non-null and non-empty; `date` and `dayOfMonth` must be null.
     * For MONTHLY: `dayOfMonth` must be non-null; `date` and `daysOfWeek` must be null.
     *
     * @param req the ReminderRequest whose schedule fields are to be validated
     * @throws InvalidScheduleException if the request's fields violate the constraints for its schedule type
     */
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

    /**
     * Ensure the request has exactly one time entry for an ONCE schedule.
     *
     * @param req the reminder request whose `times()` will be validated
     * @throws InvalidScheduleException if the request does not contain exactly one time entry
     */
    private void requireSingleTime(ReminderRequest req) {
        if (req.times().size() != 1) {
            throw new InvalidScheduleException("ONCE schedule requires exactly one time entry");
        }
    }

    /**
     * Ensures the provided value is not null.
     *
     * @param value   the value to check
     * @param message the exception message to use if the value is null
     * @throws InvalidScheduleException if {@code value} is {@code null}
     */
    private void requireNonNull(Object value, String message) {
        if (value == null) {
            throw new InvalidScheduleException(message);
        }
    }

    /**
     * Ensures the provided value is null and throws an {@link InvalidScheduleException} when it is not.
     *
     * @param value   the value that must be null
     * @param message the exception message used when the value is not null
     * @throws InvalidScheduleException if {@code value} is not null
     */
    private void requireNull(Object value, String message) {
        if (value != null) {
            throw new InvalidScheduleException(message);
        }
    }

    /**
     * Logs a warning if a reminder with the given id exists, indicating the requesting user tried to access another user's reminder.
     *
     * @param id the UUID of the reminder that was requested
     * @param requestingUser the user who attempted the access (used for logging)
     */
    private void logUnauthorizedAccessIfExists(UUID id, User requestingUser) {
        if (reminderRepository.existsById(id)) {
            log.warn("User {} attempted to access reminder {} owned by another user",
                    requestingUser.getId(), id);
        }
    }

    /**
     * Copies mutable fields from the request into the given reminder entity.
     *
     * @param reminder the reminder entity to be updated
     * @param req the request containing new field values
     */
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

    /**
     * Create a new Reminder entity populated from the given request and associated with the specified user.
     *
     * @param req  the request containing reminder data to populate the entity
     * @param user the owning user for the new reminder
     * @return     a new Reminder instance populated from `req` and linked to `user`
     */
    private Reminder toEntity(ReminderRequest req, User user) {
        Reminder r = new Reminder();
        r.setUser(user);
        applyUpdate(r, req);
        return r;
    }
}
