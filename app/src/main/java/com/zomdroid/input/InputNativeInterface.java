package com.zomdroid.input;

public class InputNativeInterface {
    public static final int VIRTUAL_CONTROLLER_ID = -1;
    public static native void sendKeyboard(int key, boolean isPressed);

    public static native void sendCursorPos(double x, double y);

    public static native void sendMouseButton(int button, boolean isPressed);

    public static native void sendJoystickAxis(int controllerId, int axis, float state);

    public static native void sendJoystickDpad(int controllerId, int dpad, char state);

    public static native void sendJoystickButton(int controllerId, int button, boolean isPressed);

    public static native void sendJoystickConnected(int controllerId, String controllerName);

    public static native void sendJoystickDisconnected(int controllerId);
}
