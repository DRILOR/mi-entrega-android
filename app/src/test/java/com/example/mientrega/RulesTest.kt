package com.example.mientrega

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class RulesTest {
    @Test fun rejectsWhitespaceNames() { assertNotNull(Rules.subjectError("   ")) }
    @Test fun acceptsAccentedSubject() { assertNull(Rules.subjectError("Tecnologías Móviles")) }
    @Test fun rejectsImpossibleDate() { assertNull(Rules.parseDate("2026-02-30")) }
    @Test fun acceptsLeapDay() { assertEquals(LocalDate.of(2028, 2, 29).toEpochDay(), Rules.parseDate("2028-02-29")) }
    @Test fun requiresSubject() { assertNotNull(Rules.activityError("Proyecto", null, "2026-10-25")) }
    @Test fun requiresTitle() { assertNotNull(Rules.activityError("  ", 1L, "2026-10-25")) }
    @Test fun requiresDate() { assertNotNull(Rules.activityError("Proyecto", 1L, "")) }
    @Test fun acceptsPastDeadlineForOverdueWork() { assertNull(Rules.activityError("Proyecto", 1L, "2020-01-01")) }
}
