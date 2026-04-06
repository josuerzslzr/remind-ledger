package com.remindledger.service;

import com.remindledger.dto.ReminderRequest;
import com.remindledger.dto.ReminderResponse;
import com.remindledger.exception.InvalidScheduleException;
import com.remindledger.exception.ReminderNotFoundException;
import com.remindledger.model.Channel;
import com.remindledger.model.Reminder;
import com.remindledger.model.ScheduleType;
import com.remindledger.model.User;
import com.remindledger.repository.ReminderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

    @Mock
    private ReminderRepository reminderRepository;

    @InjectMocks
    private ReminderService reminderService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("test-sub", "test@example.com", "Test User");
    }

    @Nested
    class OnceSchedule {

        @Test
        void validRequest_savesReminder() {
            mockSaveWithGeneratedId();

            var request = new ReminderRequest(
                    "Call dentist", ScheduleType.ONCE,
                    List.of(LocalTime.of(15, 0)),
                    LocalDate.of(2026, 4, 15),
                    null, null,
                    List.of(Channel.WEB_PUSH), null);

            ReminderResponse response = reminderService.create(request, testUser);

            assertThat(response.name()).isEqualTo("Call dentist");
            assertThat(response.scheduleType()).isEqualTo(ScheduleType.ONCE);
            assertThat(response.times()).containsExactly(LocalTime.of(15, 0));
            verify(reminderRepository).save(any());
        }

        @Test
        void missingDate_throws() {
            var request = new ReminderRequest(
                    "Test", ScheduleType.ONCE,
                    List.of(LocalTime.of(9, 0)),
                    null, null, null,
                    List.of(Channel.WEB_PUSH), null);

            assertThatThrownBy(() -> reminderService.create(request, testUser))
                    .isInstanceOf(InvalidScheduleException.class)
                    .hasMessageContaining("date is required");
            verify(reminderRepository, never()).save(any());
        }

        @Test
        void multipleTimes_throws() {
            var request = new ReminderRequest(
                    "Test", ScheduleType.ONCE,
                    List.of(LocalTime.of(9, 0), LocalTime.of(18, 0)),
                    LocalDate.of(2026, 4, 15),
                    null, null,
                    List.of(Channel.WEB_PUSH), null);

            assertThatThrownBy(() -> reminderService.create(request, testUser))
                    .isInstanceOf(InvalidScheduleException.class)
                    .hasMessageContaining("exactly one time");
            verify(reminderRepository, never()).save(any());
        }

        @Test
        void withDaysOfWeek_throws() {
            var request = new ReminderRequest(
                    "Test", ScheduleType.ONCE,
                    List.of(LocalTime.of(9, 0)),
                    LocalDate.of(2026, 4, 15),
                    List.of(DayOfWeek.MONDAY), null,
                    List.of(Channel.WEB_PUSH), null);

            assertThatThrownBy(() -> reminderService.create(request, testUser))
                    .isInstanceOf(InvalidScheduleException.class)
                    .hasMessageContaining("daysOfWeek must be null");
            verify(reminderRepository, never()).save(any());
        }

        @Test
        void withDayOfMonth_throws() {
            var request = new ReminderRequest(
                    "Test", ScheduleType.ONCE,
                    List.of(LocalTime.of(9, 0)),
                    LocalDate.of(2026, 4, 15),
                    null, 15,
                    List.of(Channel.WEB_PUSH), null);

            assertThatThrownBy(() -> reminderService.create(request, testUser))
                    .isInstanceOf(InvalidScheduleException.class);
            verify(reminderRepository, never()).save(any());
        }
    }

    @Nested
    class DailySchedule {

        @Test
        void validRequest_savesReminder() {
            mockSaveWithGeneratedId();

            var request = new ReminderRequest(
                    "Take vitamins", ScheduleType.DAILY,
                    List.of(LocalTime.of(9, 0), LocalTime.of(21, 0)),
                    null, null, null,
                    List.of(Channel.WEB_PUSH, Channel.EMAIL), null);

            ReminderResponse response = reminderService.create(request, testUser);

            assertThat(response.name()).isEqualTo("Take vitamins");
            assertThat(response.times()).hasSize(2);
            verify(reminderRepository).save(any());
        }

        @Test
        void withDate_throws() {
            var request = new ReminderRequest(
                    "Test", ScheduleType.DAILY,
                    List.of(LocalTime.of(9, 0)),
                    LocalDate.of(2026, 4, 15),
                    null, null,
                    List.of(Channel.WEB_PUSH), null);

            assertThatThrownBy(() -> reminderService.create(request, testUser))
                    .isInstanceOf(InvalidScheduleException.class)
                    .hasMessageContaining("date must be null");
        }

        @Test
        void withDaysOfWeek_throws() {
            var request = new ReminderRequest(
                    "Test", ScheduleType.DAILY,
                    List.of(LocalTime.of(9, 0)),
                    null, List.of(DayOfWeek.MONDAY), null,
                    List.of(Channel.WEB_PUSH), null);

            assertThatThrownBy(() -> reminderService.create(request, testUser))
                    .isInstanceOf(InvalidScheduleException.class);
            verify(reminderRepository, never()).save(any());
        }

        @Test
        void withDayOfMonth_throws() {
            var request = new ReminderRequest(
                    "Test", ScheduleType.DAILY,
                    List.of(LocalTime.of(9, 0)),
                    null, null, 15,
                    List.of(Channel.WEB_PUSH), null);

            assertThatThrownBy(() -> reminderService.create(request, testUser))
                    .isInstanceOf(InvalidScheduleException.class);
            verify(reminderRepository, never()).save(any());
        }
    }

    @Nested
    class WeeklySchedule {

        @Test
        void validRequest_savesReminder() {
            mockSaveWithGeneratedId();

            var request = new ReminderRequest(
                    "Team standup", ScheduleType.WEEKLY,
                    List.of(LocalTime.of(10, 0)),
                    null,
                    List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                    null,
                    List.of(Channel.WEB_PUSH), null);

            ReminderResponse response = reminderService.create(request, testUser);

            assertThat(response.name()).isEqualTo("Team standup");
            assertThat(response.daysOfWeek()).hasSize(3);
            verify(reminderRepository).save(any());
        }

        @Test
        void missingDaysOfWeek_throws() {
            var request = new ReminderRequest(
                    "Test", ScheduleType.WEEKLY,
                    List.of(LocalTime.of(10, 0)),
                    null, null, null,
                    List.of(Channel.WEB_PUSH), null);

            assertThatThrownBy(() -> reminderService.create(request, testUser))
                    .isInstanceOf(InvalidScheduleException.class)
                    .hasMessageContaining("daysOfWeek is required");
        }

        @Test
        void emptyDaysOfWeek_throws() {
            var request = new ReminderRequest(
                    "Test", ScheduleType.WEEKLY,
                    List.of(LocalTime.of(10, 0)),
                    null, List.of(), null,
                    List.of(Channel.WEB_PUSH), null);

            assertThatThrownBy(() -> reminderService.create(request, testUser))
                    .isInstanceOf(InvalidScheduleException.class)
                    .hasMessageContaining("must not be empty");
        }

        @Test
        void withDate_throws() {
            var request = new ReminderRequest(
                    "Test", ScheduleType.WEEKLY,
                    List.of(LocalTime.of(10, 0)),
                    LocalDate.of(2026, 4, 15),
                    List.of(DayOfWeek.MONDAY), null,
                    List.of(Channel.WEB_PUSH), null);

            assertThatThrownBy(() -> reminderService.create(request, testUser))
                    .isInstanceOf(InvalidScheduleException.class);
            verify(reminderRepository, never()).save(any());
        }

        @Test
        void withDayOfMonth_throws() {
            var request = new ReminderRequest(
                    "Test", ScheduleType.WEEKLY,
                    List.of(LocalTime.of(10, 0)),
                    null, List.of(DayOfWeek.MONDAY), 15,
                    List.of(Channel.WEB_PUSH), null);

            assertThatThrownBy(() -> reminderService.create(request, testUser))
                    .isInstanceOf(InvalidScheduleException.class);
            verify(reminderRepository, never()).save(any());
        }
    }

    @Nested
    class MonthlySchedule {

        @Test
        void validRequest_savesReminder() {
            mockSaveWithGeneratedId();

            var request = new ReminderRequest(
                    "Pay rent", ScheduleType.MONTHLY,
                    List.of(LocalTime.of(9, 0)),
                    null, null, 1,
                    List.of(Channel.WEB_PUSH, Channel.EMAIL), null);

            ReminderResponse response = reminderService.create(request, testUser);

            assertThat(response.name()).isEqualTo("Pay rent");
            assertThat(response.dayOfMonth()).isEqualTo(1);
            verify(reminderRepository).save(any());
        }

        @Test
        void missingDayOfMonth_throws() {
            var request = new ReminderRequest(
                    "Test", ScheduleType.MONTHLY,
                    List.of(LocalTime.of(9, 0)),
                    null, null, null,
                    List.of(Channel.WEB_PUSH), null);

            assertThatThrownBy(() -> reminderService.create(request, testUser))
                    .isInstanceOf(InvalidScheduleException.class)
                    .hasMessageContaining("dayOfMonth is required");
        }

        @Test
        void withDate_throws() {
            var request = new ReminderRequest(
                    "Test", ScheduleType.MONTHLY,
                    List.of(LocalTime.of(9, 0)),
                    LocalDate.of(2026, 4, 15), null, 1,
                    List.of(Channel.WEB_PUSH), null);

            assertThatThrownBy(() -> reminderService.create(request, testUser))
                    .isInstanceOf(InvalidScheduleException.class);
            verify(reminderRepository, never()).save(any());
        }

        @Test
        void withDaysOfWeek_throws() {
            var request = new ReminderRequest(
                    "Test", ScheduleType.MONTHLY,
                    List.of(LocalTime.of(9, 0)),
                    null, List.of(DayOfWeek.MONDAY), 1,
                    List.of(Channel.WEB_PUSH), null);

            assertThatThrownBy(() -> reminderService.create(request, testUser))
                    .isInstanceOf(InvalidScheduleException.class);
            verify(reminderRepository, never()).save(any());
        }
    }

    @Nested
    class ListForUser {

        @Test
        void multipleReminders_returnsAll() {
            Reminder r1 = buildReminder(UUID.randomUUID(), "Vitamins", testUser);
            Reminder r2 = buildReminder(UUID.randomUUID(), "Pay rent", testUser);
            when(reminderRepository.findAllByUser(testUser)).thenReturn(List.of(r1, r2));

            List<ReminderResponse> result = reminderService.listForUser(testUser);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(ReminderResponse::name)
                    .containsExactly("Vitamins", "Pay rent");
        }

        @Test
        void noReminders_returnsEmpty() {
            when(reminderRepository.findAllByUser(testUser)).thenReturn(List.of());

            assertThat(reminderService.listForUser(testUser)).isEmpty();
        }
    }

    @Nested
    class GetById {

        @Test
        void existingReminder_returnsResponse() {
            UUID id = UUID.randomUUID();

            when(reminderRepository.findByIdAndUser(id, testUser))
                    .thenReturn(Optional.of(buildReminder(id, "Dentist", testUser)));

            ReminderResponse result = reminderService.getById(id, testUser);

            assertThat(result.id()).isEqualTo(id);
            assertThat(result.name()).isEqualTo("Dentist");
        }

        @Test
        void notFound_throws() {
            UUID id = UUID.randomUUID();

            when(reminderRepository.findByIdAndUser(id, testUser)).thenReturn(Optional.empty());
            when(reminderRepository.existsById(id)).thenReturn(false);

            assertThatThrownBy(() -> reminderService.getById(id, testUser))
                    .isInstanceOf(ReminderNotFoundException.class);
        }

        @Test
        void belongsToAnotherUser_throwsNotFoundAndLogsWarning() {
            UUID id = UUID.randomUUID();

            when(reminderRepository.findByIdAndUser(id, testUser)).thenReturn(Optional.empty());
            when(reminderRepository.existsById(id)).thenReturn(true);

            assertThatThrownBy(() -> reminderService.getById(id, testUser))
                    .isInstanceOf(ReminderNotFoundException.class);
            verify(reminderRepository).existsById(id);
        }
    }

    @Nested
    class Update {

        @Test
        void validRequest_updatesAndReturnsResponse() {
            UUID id = UUID.randomUUID();
            Reminder existing = buildReminder(id, "Old name", testUser);

            when(reminderRepository.findByIdAndUser(id, testUser)).thenReturn(Optional.of(existing));
            when(reminderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var request = new ReminderRequest(
                    "New name", ScheduleType.DAILY,
                    List.of(LocalTime.of(8, 0)),
                    null, null, null,
                    List.of(Channel.IN_APP), null);

            ReminderResponse result = reminderService.update(id, request, testUser);

            assertThat(result.name()).isEqualTo("New name");
            assertThat(result.scheduleType()).isEqualTo(ScheduleType.DAILY);
            verify(reminderRepository).save(existing);
        }

        @Test
        void notFound_throws() {
            UUID id = UUID.randomUUID();

            when(reminderRepository.findByIdAndUser(id, testUser)).thenReturn(Optional.empty());
            when(reminderRepository.existsById(id)).thenReturn(false);

            var request = new ReminderRequest(
                    "Test", ScheduleType.DAILY,
                    List.of(LocalTime.of(8, 0)),
                    null, null, null,
                    List.of(Channel.IN_APP), null);

            assertThatThrownBy(() -> reminderService.update(id, request, testUser))
                    .isInstanceOf(ReminderNotFoundException.class);
            verify(reminderRepository, never()).save(any());
        }

        @Test
        void invalidSchedule_throwsWithoutSaving() {
            UUID id = UUID.randomUUID();
            Reminder existing = buildReminder(id, "Old", testUser);

            when(reminderRepository.findByIdAndUser(id, testUser)).thenReturn(Optional.of(existing));

            var request = new ReminderRequest(
                    "Test", ScheduleType.ONCE,
                    List.of(LocalTime.of(8, 0)),
                    null, null, null,
                    List.of(Channel.IN_APP), null);

            assertThatThrownBy(() -> reminderService.update(id, request, testUser))
                    .isInstanceOf(InvalidScheduleException.class)
                    .hasMessageContaining("date is required");
            verify(reminderRepository, never()).save(any());
        }
    }

    @Nested
    class Delete {

        @Test
        void existingReminder_deletesSuccessfully() {
            UUID id = UUID.randomUUID();
            Reminder reminder = buildReminder(id, "Dentist", testUser);

            when(reminderRepository.findByIdAndUser(id, testUser)).thenReturn(Optional.of(reminder));

            reminderService.delete(id, testUser);

            verify(reminderRepository).delete(reminder);
        }

        @Test
        void notFound_throws() {
            UUID id = UUID.randomUUID();

            when(reminderRepository.findByIdAndUser(id, testUser)).thenReturn(Optional.empty());
            when(reminderRepository.existsById(id)).thenReturn(false);

            assertThatThrownBy(() -> reminderService.delete(id, testUser))
                    .isInstanceOf(ReminderNotFoundException.class);
            verify(reminderRepository, never()).delete(any());
        }

        @Test
        void belongsToAnotherUser_throwsNotFoundAndLogsWarning() {
            UUID id = UUID.randomUUID();

            when(reminderRepository.findByIdAndUser(id, testUser)).thenReturn(Optional.empty());
            when(reminderRepository.existsById(id)).thenReturn(true);

            assertThatThrownBy(() -> reminderService.delete(id, testUser))
                    .isInstanceOf(ReminderNotFoundException.class);
            verify(reminderRepository, never()).delete(any());
            verify(reminderRepository).existsById(id);
        }
    }

    private void mockSaveWithGeneratedId() {
        when(reminderRepository.save(any())).thenAnswer(inv -> {
            Reminder r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });
    }

    private Reminder buildReminder(UUID id, String name, User user) {
        Reminder r = new Reminder();
        r.setId(id);
        r.setUser(user);
        r.setName(name);
        r.setScheduleType(ScheduleType.DAILY);
        r.setTimes(new java.time.LocalTime[]{LocalTime.of(8, 0)});
        r.setChannels(new Channel[]{Channel.IN_APP});
        return r;
    }
}