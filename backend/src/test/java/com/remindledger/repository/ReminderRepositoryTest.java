package com.remindledger.repository;

import com.remindledger.model.Channel;
import com.remindledger.model.Reminder;
import com.remindledger.model.ScheduleType;
import com.remindledger.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.remindledger.config.JpaConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = NONE)
@Import(JpaConfig.class)
class ReminderRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        userA = userRepository.save(new User("sub-a", "a@test.com", "User A"));
        userB = userRepository.save(new User("sub-b", "b@test.com", "User B"));
    }

    @Nested
    class FindAllByUser {

        @Test
        void multipleReminders_returnsOnlyUserReminders() {
            reminderRepository.save(buildReminder("Vitamins", userA));
            reminderRepository.save(buildReminder("Pay rent", userA));
            reminderRepository.save(buildReminder("Other user reminder", userB));

            List<Reminder> results = reminderRepository.findAllByUser(userA);

            assertThat(results).hasSize(2);
            assertThat(results).extracting(Reminder::getName)
                    .containsExactlyInAnyOrder("Vitamins", "Pay rent");
        }

        @Test
        void noReminders_returnsEmpty() {
            reminderRepository.save(buildReminder("Other user reminder", userB));

            assertThat(reminderRepository.findAllByUser(userA)).isEmpty();
        }

        @Test
        void emptyTable_returnsEmpty() {
            assertThat(reminderRepository.findAllByUser(userA)).isEmpty();
        }
    }

    @Nested
    class FindByIdAndUser {

        @Test
        void correctUser_returnsReminder() {
            Reminder saved = reminderRepository.save(buildReminder("Dentist", userA));

            Optional<Reminder> result = reminderRepository.findByIdAndUser(saved.getId(), userA);

            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Dentist");
        }

        @Test
        void wrongUser_returnsEmpty() {
            Reminder saved = reminderRepository.save(buildReminder("Dentist", userA));

            Optional<Reminder> result = reminderRepository.findByIdAndUser(saved.getId(), userB);

            assertThat(result).isEmpty();
        }

        @Test
        void nonExistentId_returnsEmpty() {
            Optional<Reminder> result = reminderRepository.findByIdAndUser(
                    java.util.UUID.randomUUID(), userA);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class ArrayColumns {

        @Test
        void times_persistsAndLoadsCorrectly() {
            Reminder saved = reminderRepository.save(buildReminder("Test", userA,
                    new LocalTime[]{LocalTime.of(8, 0), LocalTime.of(20, 0)},
                    new Channel[]{Channel.IN_APP, Channel.EMAIL}));
            entityManager.flush();
            entityManager.clear();

            Reminder loaded = reminderRepository.findById(saved.getId()).orElseThrow();

            assertThat(loaded.getTimes()).containsExactly(LocalTime.of(8, 0), LocalTime.of(20, 0));
        }

        @Test
        void daysOfWeek_persistsAndLoadsCorrectly() {
            Reminder r = new Reminder();
            r.setUser(userA);
            r.setName("Weekly test");
            r.setScheduleType(ScheduleType.WEEKLY);
            r.setTimes(new LocalTime[]{LocalTime.of(10, 0)});
            r.setChannels(new Channel[]{Channel.WEB_PUSH});
            r.setDaysOfWeek(new DayOfWeek[]{DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY});
            Reminder saved = reminderRepository.save(r);
            entityManager.flush();
            entityManager.clear();

            Reminder loaded = reminderRepository.findById(saved.getId()).orElseThrow();

            assertThat(loaded.getDaysOfWeek()).containsExactly(
                    DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
        }

        @Test
        void channels_persistsAndLoadsCorrectly() {
            Reminder saved = reminderRepository.save(buildReminder("Test", userA,
                    new LocalTime[]{LocalTime.of(9, 0)},
                    new Channel[]{Channel.IN_APP, Channel.EMAIL, Channel.WEB_PUSH}));
            entityManager.flush();
            entityManager.clear();

            Reminder loaded = reminderRepository.findById(saved.getId()).orElseThrow();

            assertThat(loaded.getChannels()).containsExactly(Channel.IN_APP, Channel.EMAIL, Channel.WEB_PUSH);
        }
    }

    private Reminder buildReminder(String name, User user) {
        return buildReminder(name, user,
                new LocalTime[]{LocalTime.of(9, 0)},
                new Channel[]{Channel.IN_APP});
    }

    private Reminder buildReminder(String name, User user, LocalTime[] times, Channel[] channels) {
        Reminder r = new Reminder();
        r.setUser(user);
        r.setName(name);
        r.setScheduleType(ScheduleType.DAILY);
        r.setTimes(times);
        r.setChannels(channels);
        return r;
    }
}
