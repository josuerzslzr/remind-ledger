package com.remindledger.repository;

import com.remindledger.model.Reminder;
import com.remindledger.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

    List<Reminder> findAllByUser(User user);

    Optional<Reminder> findByIdAndUser(UUID id, User user);
}
