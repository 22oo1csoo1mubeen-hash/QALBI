package com.wakeus.app

import com.wakeus.app.ui.home.GreetingHelper
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class GreetingHelperTest {

    @Test
    fun testMorningBoundaries() {
        assertEquals("Good morning", GreetingHelper.getGreeting(LocalTime.of(5, 0)))
        assertEquals("Good morning", GreetingHelper.getGreeting(LocalTime.of(8, 30)))
        assertEquals("Good morning", GreetingHelper.getGreeting(LocalTime.of(11, 59)))
    }

    @Test
    fun testAfternoonBoundaries() {
        assertEquals("Good afternoon", GreetingHelper.getGreeting(LocalTime.of(12, 0)))
        assertEquals("Good afternoon", GreetingHelper.getGreeting(LocalTime.of(14, 15)))
        assertEquals("Good afternoon", GreetingHelper.getGreeting(LocalTime.of(16, 59)))
    }

    @Test
    fun testEveningBoundaries() {
        assertEquals("Good evening", GreetingHelper.getGreeting(LocalTime.of(17, 0)))
        assertEquals("Good evening", GreetingHelper.getGreeting(LocalTime.of(19, 0)))
        assertEquals("Good evening", GreetingHelper.getGreeting(LocalTime.of(20, 59)))
    }

    @Test
    fun testNightBoundaries() {
        assertEquals("Good night", GreetingHelper.getGreeting(LocalTime.of(21, 0)))
        assertEquals("Good night", GreetingHelper.getGreeting(LocalTime.of(23, 59)))
        assertEquals("Good night", GreetingHelper.getGreeting(LocalTime.of(0, 0)))
        assertEquals("Good night", GreetingHelper.getGreeting(LocalTime.of(4, 59)))
    }
}
