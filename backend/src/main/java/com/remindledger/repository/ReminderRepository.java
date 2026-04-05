package com.remindledger.repository;

import com.remindledger.model.Reminder;
import com.remindledger.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

    /**
 * Finds all reminders belonging to the specified user.
 *
 * @param user the owner whose reminders should be retrieved
 * @return a list of Reminder entities associated with the given user; an empty list if none are found
 */
List<Reminder> findAllByUser(User user);

    /**
 * Find a reminder by its UUID that belongs to the specified user.
 *
 * @param id   the UUID of the reminder to find
 * @param user the owner of the reminder
 * @return an Optional containing the matching Reminder, or empty if no match is found
 */
Optional<Reminder> findByIdAndUser(UUID id, User user);
}
