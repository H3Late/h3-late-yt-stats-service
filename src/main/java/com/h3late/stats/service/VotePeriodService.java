package com.h3late.stats.service;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class VotePeriodService {

    private static final Map<String, DayOfWeek> DAY_MAP = Map.of(
        "MON", DayOfWeek.MONDAY,
        "TUE", DayOfWeek.TUESDAY,
        "WED", DayOfWeek.WEDNESDAY,
        "THU", DayOfWeek.THURSDAY,
        "FRI", DayOfWeek.FRIDAY,
        "SAT", DayOfWeek.SATURDAY,
        "SUN", DayOfWeek.SUNDAY
    );

    /**
     * Returns the UTC midnight that started the current vote period.
     * For DAILY: today's midnight. For specific days (e.g. "MON,THU"): the most recent
     * matching day's midnight, which is when this period's budget became available.
     */
    public Instant getCurrentPeriodStart(String voteRefreshSchedule) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        if ("DAILY".equalsIgnoreCase(voteRefreshSchedule.trim())) {
            return today.atStartOfDay(ZoneOffset.UTC).toInstant();
        }

        List<DayOfWeek> days = parseDays(voteRefreshSchedule);
        for (int i = 0; i <= 6; i++) {
            LocalDate candidate = today.minusDays(i);
            if (days.contains(candidate.getDayOfWeek())) {
                return candidate.atStartOfDay(ZoneOffset.UTC).toInstant();
            }
        }

        return today.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    /**
     * Returns when the next vote period starts — used by the client countdown timer.
     */
    public Instant getNextPeriodStart(String voteRefreshSchedule) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        if ("DAILY".equalsIgnoreCase(voteRefreshSchedule.trim())) {
            return today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        }

        List<DayOfWeek> days = parseDays(voteRefreshSchedule);
        for (int i = 1; i <= 7; i++) {
            LocalDate candidate = today.plusDays(i);
            if (days.contains(candidate.getDayOfWeek())) {
                return candidate.atStartOfDay(ZoneOffset.UTC).toInstant();
            }
        }

        return today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private List<DayOfWeek> parseDays(String schedule) {
        return Arrays.stream(schedule.split(","))
            .map(String::trim)
            .map(String::toUpperCase)
            .map(d -> {
                DayOfWeek dow = DAY_MAP.get(d);
                if (dow == null) throw new IllegalArgumentException("Unknown day abbreviation: " + d);
                return dow;
            })
            .toList();
    }
}
