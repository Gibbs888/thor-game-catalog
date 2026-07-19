package com.gibbstech.thorgamecatalog

import org.junit.Assert.assertEquals
import org.junit.Test

class ControllerInputTest {
    @Test
    fun ignoresAnalogNoiseInsideDeadZone() {
        assertEquals(ControllerDirection.NONE, controllerDirection(0.2f, -0.3f))
    }

    @Test
    fun mapsHorizontalAnalogMovement() {
        assertEquals(ControllerDirection.LEFT, controllerDirection(-0.9f, 0.1f))
        assertEquals(ControllerDirection.RIGHT, controllerDirection(0.9f, 0.1f))
    }

    @Test
    fun mapsVerticalAnalogMovement() {
        assertEquals(ControllerDirection.UP, controllerDirection(0.1f, -0.9f))
        assertEquals(ControllerDirection.DOWN, controllerDirection(0.1f, 0.9f))
    }

    @Test
    fun usesDominantAxisForDiagonalMovement() {
        assertEquals(ControllerDirection.RIGHT, controllerDirection(0.9f, 0.7f))
        assertEquals(ControllerDirection.UP, controllerDirection(0.7f, -0.9f))
    }
}
