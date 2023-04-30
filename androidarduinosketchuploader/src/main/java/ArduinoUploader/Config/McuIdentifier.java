package ArduinoUploader.Config;

import ArduinoUploader.*;

public enum McuIdentifier {
    AtMega1284,
    AtMega1284P,
    AtMega2560,
    AtMega32U4,
    AtMega168,
    AtMega328P;
    //1280
    //

    public static final int SIZE = java.lang.Integer.SIZE;

    public int getValue() {
        return this.ordinal();
    }

    public static McuIdentifier forValue(int value) {
        return values()[value];
    }
}