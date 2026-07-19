package com.gibbstech.thorgamecatalog

import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    private var lastAnalogDirection = KeyEvent.KEYCODE_UNKNOWN
    private var lastAnalogMoveAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ThorCatalogTheme {
                GameCatalogApp()
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val mappedKeyCode = when (event.keyCode) {
            KeyEvent.KEYCODE_BUTTON_A -> KeyEvent.KEYCODE_DPAD_CENTER
            KeyEvent.KEYCODE_BUTTON_B -> KeyEvent.KEYCODE_BACK
            else -> return super.dispatchKeyEvent(event)
        }

        return super.dispatchKeyEvent(event.withKeyCode(mappedKeyCode))
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        val isController = event.source and InputDevice.SOURCE_JOYSTICK ==
            InputDevice.SOURCE_JOYSTICK
        if (!isController || event.action != MotionEvent.ACTION_MOVE) {
            return super.dispatchGenericMotionEvent(event)
        }

        val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
        val x = hatX.takeIf { abs(it) >= DEAD_ZONE }
            ?: event.getAxisValue(MotionEvent.AXIS_X)
        val y = hatY.takeIf { abs(it) >= DEAD_ZONE }
            ?: event.getAxisValue(MotionEvent.AXIS_Y)
        val direction = when (controllerDirection(x, y, DEAD_ZONE)) {
            ControllerDirection.NONE -> KeyEvent.KEYCODE_UNKNOWN
            ControllerDirection.LEFT -> KeyEvent.KEYCODE_DPAD_LEFT
            ControllerDirection.RIGHT -> KeyEvent.KEYCODE_DPAD_RIGHT
            ControllerDirection.UP -> KeyEvent.KEYCODE_DPAD_UP
            ControllerDirection.DOWN -> KeyEvent.KEYCODE_DPAD_DOWN
        }

        if (direction == KeyEvent.KEYCODE_UNKNOWN) {
            lastAnalogDirection = direction
            return super.dispatchGenericMotionEvent(event)
        }

        val now = event.eventTime
        if (direction != lastAnalogDirection || now - lastAnalogMoveAt >= ANALOG_REPEAT_MS) {
            lastAnalogDirection = direction
            lastAnalogMoveAt = now
            dispatchDirectionalKey(direction, now)
        }
        return true
    }

    private fun dispatchDirectionalKey(keyCode: Int, eventTime: Long) {
        super.dispatchKeyEvent(
            KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0),
        )
        super.dispatchKeyEvent(
            KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0),
        )
    }

    private fun KeyEvent.withKeyCode(keyCode: Int): KeyEvent = KeyEvent(
        downTime,
        eventTime,
        action,
        keyCode,
        repeatCount,
        metaState,
        deviceId,
        scanCode,
        flags,
        source,
    )

    private companion object {
        const val DEAD_ZONE = 0.55f
        const val ANALOG_REPEAT_MS = 180L
    }
}
