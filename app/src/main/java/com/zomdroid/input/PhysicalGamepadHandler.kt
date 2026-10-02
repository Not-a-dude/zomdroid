package com.zomdroid.input

import android.hardware.input.InputManager
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import kotlin.math.abs

class PhysicalGamepadHandler : InputManager.InputDeviceListener {

    private var currentDpadState = 0
    private val connectedDeviceIds = mutableSetOf<Int>()
    private var lastAxisLogTime = 0L

    private var lastLx = 0f
    private var lastLy = 0f
    private var lastRx = 0f
    private var lastRy = 0f
    private var lastLt = 0f
    private var lastRt = 0f
    private var lastHatState = 0

    fun onKeyEvent(event: KeyEvent): Boolean {
        val device = event.device
        val keyCode = event.keyCode

        if (!isGamepadDevice(device) && !isGamepadKey(keyCode)) return false

        val deviceId = event.deviceId
        val isDown = event.action == KeyEvent.ACTION_DOWN
        val isUp = event.action == KeyEvent.ACTION_UP

        val button = mapKeyCodeToButton(keyCode)
        val dpadState = getDpadStateFromKeys(keyCode)

        if (button != -1) {
            Log.d(
                "ZomdroidGamepad",
                "KEY EVENT: deviceId=$deviceId action=${if (isDown) "DOWN" else if (isUp) "UP" else event.action} code=$keyCode (${KeyEvent.keyCodeToString(keyCode)}) -> button=$button"
            )
            if (isDown || isUp) {
                InputNativeInterface.sendJoystickButton(deviceId, button, isDown)
            }
            return true
        }

        if (dpadState != -1) {
            if (isDown) {
                currentDpadState = currentDpadState or dpadState
            } else if (isUp) {
                currentDpadState = currentDpadState and dpadState.inv()
            }
            Log.d(
                "ZomdroidGamepad",
                "DPAD KEY EVENT: deviceId=$deviceId action=${if (isDown) "DOWN" else if (isUp) "UP" else event.action} code=$keyCode (${KeyEvent.keyCodeToString(keyCode)}) -> dpadState=$currentDpadState"
            )
            InputNativeInterface.sendJoystickDpad(deviceId, 0, currentDpadState.toChar())
            return true
        }

        return false
    }

    fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return onKeyEvent(event)
    }

    fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return onKeyEvent(event)
    }

    fun onGenericMotionEvent(event: MotionEvent): Boolean {
        val device = event.device
        if (!isGamepadDevice(device)) return false
        if (event.action != MotionEvent.ACTION_MOVE) return false

        val deviceId = event.deviceId

        // Left stick
        val lx = event.getAxisValue(MotionEvent.AXIS_X)
        val ly = event.getAxisValue(MotionEvent.AXIS_Y)
        InputNativeInterface.sendJoystickAxis(deviceId, GLFWBinding.GAMEPAD_AXIS_LX.code, lx)
        InputNativeInterface.sendJoystickAxis(deviceId, GLFWBinding.GAMEPAD_AXIS_LY.code, ly)

        // Right stick
        var rx = event.getAxisValue(MotionEvent.AXIS_Z)
        var ry = event.getAxisValue(MotionEvent.AXIS_RZ)
        if (rx == 0f && ry == 0f) {
            rx = event.getAxisValue(MotionEvent.AXIS_RX)
            ry = event.getAxisValue(MotionEvent.AXIS_RY)
        }
        InputNativeInterface.sendJoystickAxis(deviceId, GLFWBinding.GAMEPAD_AXIS_RX.code, rx)
        InputNativeInterface.sendJoystickAxis(deviceId, GLFWBinding.GAMEPAD_AXIS_RY.code, ry)

        // Triggers
        val lt = event.getAxisValue(MotionEvent.AXIS_BRAKE).let { if (it == 0f) event.getAxisValue(MotionEvent.AXIS_LTRIGGER) else it }
        val rt = event.getAxisValue(MotionEvent.AXIS_GAS).let { if (it == 0f) event.getAxisValue(MotionEvent.AXIS_RTRIGGER) else it }
        InputNativeInterface.sendJoystickAxis(deviceId, GLFWBinding.GAMEPAD_AXIS_LT.code, lt)
        InputNativeInterface.sendJoystickAxis(deviceId, GLFWBinding.GAMEPAD_AXIS_RT.code, rt)

        // Dpad from axes (Hat)
        val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)

        var hatState = 0
        if (hatY < -0.5f) hatState = hatState or 0x1 // UP
        if (hatX > 0.5f) hatState = hatState or 0x2  // RIGHT
        if (hatY > 0.5f) hatState = hatState or 0x4  // DOWN
        if (hatX < -0.5f) hatState = hatState or 0x8 // LEFT

        val mergedDpad = (hatState or currentDpadState).toChar()
        InputNativeInterface.sendJoystickDpad(deviceId, 0, mergedDpad)

        val now = System.currentTimeMillis()
        val hasAxisChanged = abs(lx - lastLx) > 0.05f ||
                abs(ly - lastLy) > 0.05f ||
                abs(rx - lastRx) > 0.05f ||
                abs(ry - lastRy) > 0.05f ||
                abs(lt - lastLt) > 0.05f ||
                abs(rt - lastRt) > 0.05f ||
                hatState != lastHatState

        if (hasAxisChanged && (now - lastAxisLogTime > 150L)) {
            lastAxisLogTime = now
            lastLx = lx
            lastLy = ly
            lastRx = rx
            lastRy = ry
            lastLt = lt
            lastRt = rt
            lastHatState = hatState
            Log.d(
                "ZomdroidGamepad",
                "AXIS CHANGE: deviceId=$deviceId LX=${"%.2f".format(lx)} LY=${"%.2f".format(ly)} RX=${"%.2f".format(rx)} RY=${"%.2f".format(ry)} LT=${"%.2f".format(lt)} RT=${"%.2f".format(rt)} HatX=$hatX HatY=$hatY (dpad=${mergedDpad.code})"
            )
        }

        return true
    }

    fun notifyAlreadyConnectedDevices(inputManager: InputManager) {
        val deviceIds = inputManager.inputDeviceIds
        Log.d("ZomdroidGamepad", "notifyAlreadyConnectedDevices checking ${deviceIds.size} devices")
        for (id in deviceIds) {
            val device = inputManager.getInputDevice(id) ?: continue
            Log.d("ZomdroidGamepad", "Device id=$id name='${device.name}' sources=0x${Integer.toHexString(device.sources)} isGamepad=${isGamepadDevice(device)}")
            if (isGamepadDevice(device)) {
                connectDevice(id)
            }
        }
    }

    private fun isGamepadDevice(device: InputDevice?): Boolean {
        if (device == null || device.isVirtual) return false

        val sources = device.sources

        val isGamepadSource = (sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK ||
                (sources and InputDevice.SOURCE_CLASS_JOYSTICK) == InputDevice.SOURCE_CLASS_JOYSTICK

        if (!isGamepadSource) return false

        // 1. Left stick (AXIS_X & AXIS_Y)
        val hasLeftStick = device.getMotionRange(MotionEvent.AXIS_X) != null &&
                device.getMotionRange(MotionEvent.AXIS_Y) != null

        // 2. Right stick (AXIS_Z & AXIS_RZ, or AXIS_RX & AXIS_RY)
        val hasRightStick = (device.getMotionRange(MotionEvent.AXIS_Z) != null && device.getMotionRange(MotionEvent.AXIS_RZ) != null) ||
                (device.getMotionRange(MotionEvent.AXIS_RX) != null && device.getMotionRange(MotionEvent.AXIS_RY) != null)

        // 3. D-pad (HAT axes or D-pad physical keys)
        val hasHatDpad = device.getMotionRange(MotionEvent.AXIS_HAT_X) != null &&
                device.getMotionRange(MotionEvent.AXIS_HAT_Y) != null
        val hasKeyDpad = device.hasKeys(
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT
        ).all { it }
        val hasDpad = hasHatDpad || hasKeyDpad

        // 4. Face buttons (A, B, X, Y or 1, 2, 3, 4)
        val hasStandardButtons = device.hasKeys(
            KeyEvent.KEYCODE_BUTTON_A,
            KeyEvent.KEYCODE_BUTTON_B,
            KeyEvent.KEYCODE_BUTTON_X,
            KeyEvent.KEYCODE_BUTTON_Y
        ).all { it }
        val hasNumberedButtons = device.hasKeys(
            KeyEvent.KEYCODE_BUTTON_1,
            KeyEvent.KEYCODE_BUTTON_2,
            KeyEvent.KEYCODE_BUTTON_3,
            KeyEvent.KEYCODE_BUTTON_4
        ).all { it }
        val hasFaceButtons = hasStandardButtons || hasNumberedButtons

        // 5. Triggers / Bumpers (Analog LT/RT or L1/R1/L2/R2 buttons)
        val hasAnalogTriggers = (device.getMotionRange(MotionEvent.AXIS_LTRIGGER) != null || device.getMotionRange(MotionEvent.AXIS_BRAKE) != null) &&
                (device.getMotionRange(MotionEvent.AXIS_RTRIGGER) != null || device.getMotionRange(MotionEvent.AXIS_GAS) != null)
        val hasButtonTriggers = device.hasKeys(
            KeyEvent.KEYCODE_BUTTON_L2,
            KeyEvent.KEYCODE_BUTTON_R2
        ).all { it } || device.hasKeys(
            KeyEvent.KEYCODE_BUTTON_L1,
            KeyEvent.KEYCODE_BUTTON_R1
        ).all { it } || device.hasKeys(
            KeyEvent.KEYCODE_BUTTON_5,
            KeyEvent.KEYCODE_BUTTON_6
        ).all { it }
        val hasTriggers = hasAnalogTriggers || hasButtonTriggers

        return hasLeftStick && hasRightStick && hasDpad && hasFaceButtons && hasTriggers
    }

    private fun isGamepadKey(keyCode: Int): Boolean {
        return keyCode in KeyEvent.KEYCODE_BUTTON_A..KeyEvent.KEYCODE_BUTTON_16 ||
                keyCode in KeyEvent.KEYCODE_DPAD_UP..KeyEvent.KEYCODE_DPAD_CENTER
    }

    private fun getCleanDeviceName(device: InputDevice?): String {
        val rawName = device?.name?.trim()
        if (rawName.isNullOrEmpty() || rawName.equals("null", ignoreCase = true)) {
            return "Xbox Wireless Controller"
        }
        return rawName
    }

    private fun connectDevice(deviceId: Int) {
        if (connectedDeviceIds.add(deviceId)) {
            val device = InputDevice.getDevice(deviceId)
            val name = getCleanDeviceName(device)
            Log.i("ZomdroidGamepad", "Connected gamepad deviceId=$deviceId name='$name'")
            InputNativeInterface.sendJoystickConnected(deviceId, name)
        }
    }

    private fun disconnectDevice(deviceId: Int) {
        if (connectedDeviceIds.remove(deviceId)) {
            Log.i("ZomdroidGamepad", "Disconnected gamepad deviceId=$deviceId")
            currentDpadState = 0
            InputNativeInterface.sendJoystickDisconnected(deviceId)
        }
    }

    override fun onInputDeviceAdded(deviceId: Int) {
        val device = InputDevice.getDevice(deviceId)
        Log.d("ZomdroidGamepad", "onInputDeviceAdded deviceId=$deviceId name='${device?.name}'")
        if (isGamepadDevice(device)) {
            connectDevice(deviceId)
        }
    }

    override fun onInputDeviceRemoved(deviceId: Int) {
        Log.d("ZomdroidGamepad", "onInputDeviceRemoved deviceId=$deviceId")
        disconnectDevice(deviceId)
    }

    override fun onInputDeviceChanged(deviceId: Int) {
    }

    private fun mapKeyCodeToButton(keyCode: Int): Int {
        return when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_1 -> GLFWBinding.GAMEPAD_BUTTON_A.code
            KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BUTTON_2 -> GLFWBinding.GAMEPAD_BUTTON_B.code
            KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_BUTTON_3 -> GLFWBinding.GAMEPAD_BUTTON_X.code
            KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_BUTTON_4 -> GLFWBinding.GAMEPAD_BUTTON_Y.code
            KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_5 -> GLFWBinding.GAMEPAD_BUTTON_LB.code
            KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_BUTTON_6 -> GLFWBinding.GAMEPAD_BUTTON_RB.code
            KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_BUTTON_10 -> GLFWBinding.GAMEPAD_BUTTON_START.code
            KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_9 -> GLFWBinding.GAMEPAD_BUTTON_BACK.code
            KeyEvent.KEYCODE_BUTTON_MODE, KeyEvent.KEYCODE_BUTTON_11 -> GLFWBinding.GAMEPAD_BUTTON_GUIDE.code
            KeyEvent.KEYCODE_BUTTON_THUMBL, KeyEvent.KEYCODE_BUTTON_12, KeyEvent.KEYCODE_BUTTON_13 -> GLFWBinding.GAMEPAD_BUTTON_LSTICK.code
            KeyEvent.KEYCODE_BUTTON_THUMBR, KeyEvent.KEYCODE_BUTTON_14 -> GLFWBinding.GAMEPAD_BUTTON_RSTICK.code
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
}
