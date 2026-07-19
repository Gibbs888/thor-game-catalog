package com.gibbstech.thorgamecatalog

import kotlin.math.abs

enum class ControllerDirection {
    NONE,
    LEFT,
    RIGHT,
    UP,
    DOWN,
}

fun controllerDirection(
    x: Float,
    y: Float,
    deadZone: Float = 0.55f,
): ControllerDirection = when {
    abs(x) < deadZone && abs(y) < deadZone -> ControllerDirection.NONE
    abs(x) > abs(y) && x < 0 -> ControllerDirection.LEFT
    abs(x) > abs(y) -> ControllerDirection.RIGHT
    y < 0 -> ControllerDirection.UP
    else -> ControllerDirection.DOWN
}
