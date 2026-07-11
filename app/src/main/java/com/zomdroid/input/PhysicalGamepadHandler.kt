package com.zomdroid.input

import android.hardware.input.InputManager
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent

class PhysicalGamepadHandler : InputManager.InputDeviceListener {

    private var currentDpadState = 0

    fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (!isGamepadDevice(event.device)) return false

        val button = mapKeyCodeToButton(keyCode)
        if (button != -1) {
            InputNativeInterface.sendJoystickButton(button, true)
            return true
        }

        // Handle DPAD buttons if they are reported as KeyEvents
        val dpadState = getDpadStateFromKeys(keyCode)
        if (dpadState != -1) {
            currentDpadState = currentDpadState or dpadState
            InputNativeInterface.sendJoystickDpad(0, currentDpadState.toChar())
            return true
        }

        return false
    }

    fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (!isGamepadDevice(event.device)) return false

        val button = mapKeyCodeToButton(keyCode)
        if (button != -1) {
            InputNativeInterface.sendJoystickButton(button, false)
            return true
        }

        val dpadState = getDpadStateFromKeys(keyCode)
        if (dpadState != -1) {
            currentDpadState = currentDpadState and dpadState.inv()
            InputNativeInterface.sendJoystickDpad(0, currentDpadState.toChar())
            return true
        }

        return false
    }

    fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (!isGamepadDevice(event.device)) return false
        if (event.action != MotionEvent.ACTION_MOVE) return false

        // Left stick
        InputNativeInterface.sendJoystickAxis(GLFWBinding.GAMEPAD_AXIS_LX.code, event.getAxisValue(MotionEvent.AXIS_X))
        InputNativeInterface.sendJoystickAxis(GLFWBinding.GAMEPAD_AXIS_LY.code, event.getAxisValue(MotionEvent.AXIS_Y))

        // Right stick
        var rx = event.getAxisValue(MotionEvent.AXIS_Z)
        var ry = event.getAxisValue(MotionEvent.AXIS_RZ)
        if (rx == 0f && ry == 0f) {
            rx = event.getAxisValue(MotionEvent.AXIS_RX)
            ry = event.getAxisValue(MotionEvent.AXIS_RY)
        }
        InputNativeInterface.sendJoystickAxis(GLFWBinding.GAMEPAD_AXIS_RX.code, rx)
        InputNativeInterface.sendJoystickAxis(GLFWBinding.GAMEPAD_AXIS_RY.code, ry)

        // Triggers
        val lt = event.getAxisValue(MotionEvent.AXIS_BRAKE).let { if (it == 0f) event.getAxisValue(MotionEvent.AXIS_LTRIGGER) else it }
        val rt = event.getAxisValue(MotionEvent.AXIS_GAS).let { if (it == 0f) event.getAxisValue(MotionEvent.AXIS_RTRIGGER) else it }
        InputNativeInterface.sendJoystickAxis(GLFWBinding.GAMEPAD_AXIS_LT.code, lt)
        InputNativeInterface.sendJoystickAxis(GLFWBinding.GAMEPAD_AXIS_RT.code, rt)

        // Dpad from axes (Hat)
        val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)

        var hatState = 0
        if (hatY < -0.5f) hatState = hatState or 0x1 // UP
        if (hatX > 0.5f) hatState = hatState or 0x2  // RIGHT
        if (hatY > 0.5f) hatState = hatState or 0x4  // DOWN
        if (hatX < -0.5f) hatState = hatState or 0x8 // LEFT

        // Always send the merged state, otherwise releasing the hat back to (0, 0)
        // would never be reported and the dpad would appear stuck
        InputNativeInterface.sendJoystickDpad(0, (hatState or currentDpadState).toChar())

        return true
    }

    private fun isGamepadDevice(device: InputDevice?): Boolean {
        if (device == null) return false
        val sources = device.sources
        return (sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD) ||
                (sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK)
    }

    private fun mapKeyCodeToButton(keyCode: Int): Int {
        return when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_A -> GLFWBinding.GAMEPAD_BUTTON_A.code
            KeyEvent.KEYCODE_BUTTON_B -> GLFWBinding.GAMEPAD_BUTTON_B.code
            KeyEvent.KEYCODE_BUTTON_X -> GLFWBinding.GAMEPAD_BUTTON_X.code
            KeyEvent.KEYCODE_BUTTON_Y -> GLFWBinding.GAMEPAD_BUTTON_Y.code
            KeyEvent.KEYCODE_BUTTON_L1 -> GLFWBinding.GAMEPAD_BUTTON_LB.code
            KeyEvent.KEYCODE_BUTTON_R1 -> GLFWBinding.GAMEPAD_BUTTON_RB.code
            KeyEvent.KEYCODE_BUTTON_START -> GLFWBinding.GAMEPAD_BUTTON_START.code
            KeyEvent.KEYCODE_BUTTON_SELECT -> GLFWBinding.GAMEPAD_BUTTON_BACK.code
            KeyEvent.KEYCODE_BUTTON_MODE -> GLFWBinding.GAMEPAD_BUTTON_GUIDE.code
            KeyEvent.KEYCODE_BUTTON_THUMBL -> GLFWBinding.GAMEPAD_BUTTON_LSTICK.code
            KeyEvent.KEYCODE_BUTTON_THUMBR -> GLFWBinding.GAMEPAD_BUTTON_RSTICK.code
            else -> -1
        }
    }

    private fun getDpadStateFromKeys(keyCode: Int): Int {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> 0x1
            KeyEvent.KEYCODE_DPAD_RIGHT -> 0x2
            KeyEvent.KEYCODE_DPAD_DOWN -> 0x4
            KeyEvent.KEYCODE_DPAD_LEFT -> 0x8
            else -> -1
        }
    }

    override fun onInputDeviceAdded(deviceId: Int) {
        // Only notify the native side for actual gamepads, not for every input device
        if (isGamepadDevice(InputDevice.getDevice(deviceId))) {
            InputNativeInterface.sendJoystickConnected()
        }
    }

    override fun onInputDeviceRemoved(deviceId: Int) {
        // The device is already gone, so we can't check its sources here.
        // Reset everything so the native side is not left with stuck inputs.
        currentDpadState = 0
        InputNativeInterface.sendJoystickDpad(0, 0.toChar())
        InputNativeInterface.sendJoystickAxis(GLFWBinding.GAMEPAD_AXIS_LX.code, 0f)
        InputNativeInterface.sendJoystickAxis(GLFWBinding.GAMEPAD_AXIS_LY.code, 0f)
        InputNativeInterface.sendJoystickAxis(GLFWBinding.GAMEPAD_AXIS_RX.code, 0f)
        InputNativeInterface.sendJoystickAxis(GLFWBinding.GAMEPAD_AXIS_RY.code, 0f)
        InputNativeInterface.sendJoystickAxis(GLFWBinding.GAMEPAD_AXIS_LT.code, 0f)
        InputNativeInterface.sendJoystickAxis(GLFWBinding.GAMEPAD_AXIS_RT.code, 0f)
    }

    override fun onInputDeviceChanged(deviceId: Int) {
    }
}
