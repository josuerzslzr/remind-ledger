package com.remindledger.dto;

import com.remindledger.model.Channel;
import com.remindledger.model.Reminder;
import com.remindledger.model.ScheduleType;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public record ReminderResponse(
        UUID id,
        String name,
        ScheduleType scheduleType,
        List<LocalTime> times,
        LocalDate date,
        List<DayOfWeek> daysOfWeek,
        Integer dayOfMonth,
        List<Channel> channels,
        LocalDate validUntil,
        Instant createdAt,
        Instant updatedAt
) {
    public static ReminderResponse from(Reminder entity) {
        return new ReminderResponse(
                entity.getId(),
                entity.getName(),
                entity.getScheduleType(),
                Arrays.asList(entity.getTimes()),
                entity.getDate(),
                entity.getDaysOfWeek() != null ? Arrays.asList(entity.getDaysOfWeek()) : null,
                entity.getDayOfMonth(),
                Arrays.asList(entity.getChannels()),
                entity.getValidUntil(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
