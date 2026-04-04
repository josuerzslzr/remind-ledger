package com.remindledger.dto;

import com.remindledger.model.Channel;
import com.remindledger.model.ScheduleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record ReminderRequest(

        @NotBlank
        @Size(max = 255)
        String name,

        @NotNull
        ScheduleType scheduleType,

        @NotEmpty
        List<@NotNull LocalTime> times,

        LocalDate date,

        List<@NotNull DayOfWeek> daysOfWeek,

        Integer dayOfMonth,

        @NotEmpty
        List<@NotNull Channel> channels,

        LocalDate validUntil
) {}
