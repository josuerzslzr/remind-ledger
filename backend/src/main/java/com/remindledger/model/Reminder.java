package com.remindledger.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "reminders")
@EntityListeners(AuditingEntityListener.class)
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false, length = 20)
    private ScheduleType scheduleType;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "times", columnDefinition = "time[]", nullable = false)
    private LocalTime[] times;

    private LocalDate date;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "days_of_week", columnDefinition = "text[]")
    private DayOfWeek[] daysOfWeek;

    @Column(name = "day_of_month")
    private Integer dayOfMonth;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "channels", columnDefinition = "text[]", nullable = false)
    private Channel[] channels;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
 * Default no-argument constructor required by JPA and frameworks that instantiate entities via reflection.
 */
public Reminder() {}

    /**
 * Gets the entity's primary key.
 *
 * @return the UUID primary key of this reminder
 */
public UUID getId() { return id; }
    /**
 * Set the primary key UUID for this Reminder.
 *
 * @param id the UUID to assign as the entity's primary key; typically assigned by the persistence provider
 */
public void setId(UUID id) { this.id = id; }

    /**
 * Gets the user who owns this reminder.
 *
 * @return the user that owns this reminder
 */
public User getUser() { return user; }
    /**
 * Associates this reminder with the given user.
 *
 * @param user the user to associate with this reminder; must not be null
 */
public void setUser(User user) { this.user = user; }

    /**
 * Gets the reminder's name.
 *
 * @return the reminder's name
 */
public String getName() { return name; }
    /**
 * Set the reminder's name.
 *
 * @param name the name of the reminder; must not be null when persisting
 */
public void setName(String name) { this.name = name; }

    /**
 * The scheduling strategy used by this reminder.
 *
 * @return the reminder's ScheduleType
 */
public ScheduleType getScheduleType() { return scheduleType; }
    /**
 * Sets the reminder's schedule type.
 *
 * @param scheduleType the schedule type determining how the reminder is scheduled; must not be {@code null}
 */
public void setScheduleType(ScheduleType scheduleType) { this.scheduleType = scheduleType; }

    /**
 * Get the scheduled times for the reminder.
 *
 * @return an array of scheduled LocalTime values for this reminder; never null
 */
public LocalTime[] getTimes() { return times; }
    /**
 * Set the scheduled times for this reminder.
 *
 * @param times an array of LocalTime values representing the reminder times; must not be null
 */
public void setTimes(LocalTime[] times) { this.times = times; }

    /**
 * Gets the reminder's scheduled date.
 *
 * @return the scheduled date, or `null` if no specific date is set
 */
public LocalDate getDate() { return date; }
    /**
 * Set the specific calendar date for the reminder.
 *
 * @param date the date on which the reminder should occur, or {@code null} to clear it
 */
public void setDate(LocalDate date) { this.date = date; }

    /**
 * Gets the configured days of week for the reminder.
 *
 * @return an array of `DayOfWeek` values representing the reminder's scheduled days, or `null` if not set
 */
public DayOfWeek[] getDaysOfWeek() { return daysOfWeek; }
    /**
 * Sets the days of the week on which the reminder is active.
 *
 * @param daysOfWeek an array of `DayOfWeek` values specifying active weekdays, or `null` to clear the setting
 */
public void setDaysOfWeek(DayOfWeek[] daysOfWeek) { this.daysOfWeek = daysOfWeek; }

    /**
 * Gets the day-of-month for the reminder.
 *
 * @return the day of month (1–31) or {@code null} if not set
 */
public Integer getDayOfMonth() { return dayOfMonth; }
    /**
 * Set the day of the month for this reminder.
 *
 * @param dayOfMonth the day of the month (1–31), or {@code null} to unset
 */
public void setDayOfMonth(Integer dayOfMonth) { this.dayOfMonth = dayOfMonth; }

    /**
 * Gets the channels through which the reminder will be delivered.
 *
 * @return an array of `Channel` values representing the delivery channels for this reminder
 */
public Channel[] getChannels() { return channels; }
    /**
 * Set the delivery channels for this reminder.
 *
 * @param channels an array of Channel values indicating where notifications should be sent; must not be null
 */
public void setChannels(Channel[] channels) { this.channels = channels; }

    /**
 * The date after which the reminder is no longer active.
 *
 * @return the final date the reminder is valid (inclusive), or {@code null} if the reminder does not expire
 */
public LocalDate getValidUntil() { return validUntil; }
    /**
 * Sets the date after which the reminder is no longer valid.
 *
 * @param validUntil the inclusive last valid date for the reminder, or {@code null} if the reminder should not expire
 */
public void setValidUntil(LocalDate validUntil) { this.validUntil = validUntil; }

    /**
 * Timestamp when the reminder was created.
 *
 * @return the creation timestamp, populated by JPA auditing (may be null before the entity is persisted)
 */
public Instant getCreatedAt() { return createdAt; }

    /**
 * Gets the timestamp when this entity was last modified.
 *
 * @return the last modification timestamp
 */
public Instant getUpdatedAt() { return updatedAt; }
}
