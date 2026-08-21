package com.griff.subscriptions.domain.reminder

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The three switches that all have to agree before anything reaches the user. */
class ReminderAvailabilityTest {

    @Test
    fun `all three switches on delivers`() {
        assertTrue(ReminderAvailability.isEffective(true, true, true))
    }

    @Test
    fun `the record's own switch blocks its reminders`() {
        assertFalse(ReminderAvailability.isEffective(true, false, true))
    }

    @Test
    fun `the app wide switch blocks everything`() {
        assertFalse(ReminderAvailability.isEffective(false, true, true))
    }

    @Test
    fun `a system that refuses notifications blocks everything`() {
        assertFalse(ReminderAvailability.isEffective(true, true, false))
    }
}
