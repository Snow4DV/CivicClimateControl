package ru.snowadv.civic_climate_control.Adapter;

import java.util.Objects;

import ru.snowadv.civic_climate_control.R;

public final class AdapterState {
    private int fanLevel;

    private int tempLeft = 0;
    private int tempRight = 0;

    public enum ACState {
        HIDDEN, ON, OFF
    }

    public enum FanLevel {
        LEVEL_0(R.drawable.ic_fan_speed_0),
        LEVEL_1(R.drawable.ic_fan_speed_1),
        LEVEL_2(R.drawable.ic_fan_speed_2),
        LEVEL_3(R.drawable.ic_fan_speed_3),
        LEVEL_4(R.drawable.ic_fan_speed_4),
        LEVEL_5(R.drawable.ic_fan_speed_5),
        LEVEL_6(R.drawable.ic_fan_speed_6),
        LEVEL_7(R.drawable.ic_fan_speed_7);
        private final int resourceId;

        FanLevel(int resourceId) {
            this.resourceId = resourceId;
        }

        public int getResourceId() {
            return resourceId;
        }

    }
    public enum FanDirection {
        NONE(R.drawable.ic_fan_dir_none),
        UP(R.drawable.ic_fan_dir_up),
        DOWN(R.drawable.ic_fan_dir_down),
        UP_DOWN(R.drawable.ic_fan_dir_up_down),
        DOWN_WINDSHIELD(R.drawable.ic_fan_dir_down_windshield),
        WINDSHIELD(R.drawable.ic_fan_dir_windshield);

        private final int resourceId;

        public int getResourceId() {
            return resourceId;
        }

        FanDirection(int resourceId) {
            this.resourceId = resourceId;
        }
    }

    private int fanDirection;

    public FanDirection getFanDirection() {
        switch (fanDirection) {
            default:
            case 0:
                return FanDirection.NONE;
            case 1:
                return FanDirection.UP;
            case 2:
                return FanDirection.UP_DOWN;
            case 3:
                return FanDirection.DOWN;
            case 4:
                return FanDirection.DOWN_WINDSHIELD;
            case 5:
                return FanDirection.WINDSHIELD;
        }
    }

    private int ac = 0; // 0 - not shown; 1 - on; 2 - off
    private boolean auto = false;

    public FanLevel getFanLevel() {
        switch (fanLevel) {
            default:
            case 0:
                return FanLevel.LEVEL_0;
            case 1:
                return FanLevel.LEVEL_1;
            case 2:
                return FanLevel.LEVEL_2;
            case 3:
                return FanLevel.LEVEL_3;
            case 4:
                return FanLevel.LEVEL_4;
            case 5:
                return FanLevel.LEVEL_5;
            case 6:
                return FanLevel.LEVEL_6;
            case 7:
                return FanLevel.LEVEL_7;
        }
    }

    public int getTempLeft() {
        return tempLeft;
    }

    public int getTempRight() {
        return tempRight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AdapterState that = (AdapterState) o;
        return tempLeft == that.tempLeft && tempRight == that.tempRight && ac == that.ac && auto == that.auto && fanLevel == that.fanLevel && fanDirection == that.fanDirection;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fanLevel, tempLeft, tempRight, fanDirection, ac, auto);
    }

    public String getTempLeftString() {
        switch(tempLeft) {
            case -1:
                return "";
            case 0:
                return "LO";
            case 99:
                return "HI";
            default:
                return String.valueOf(tempLeft);
        }
    }

    public boolean isTempRightVisible() {
        return tempRight != -1;
    }

    public boolean isTempLeftVisible() {
        return tempLeft != -1;
    }

    public String getTempRightString() {
        switch(tempRight) {
            case -1:
                return "";
            case 0:
                return "LO";
            case 99:
                return "HI";
            default:
                return String.valueOf(tempRight);
        }
    }

    public int getAcRaw() {
        return ac;
    }

    public ACState getAcState() {
        switch(ac) {
            default:
            case 0:
                return ACState.HIDDEN;
            case 1:
                return ACState.ON;
            case 2:
                return ACState.OFF;
        }
    }



    public boolean isAuto() {
        return auto;
    }

    public AdapterState(int fanLevel, int tempLeft, int tempRight, int fanDirection, int ac, boolean auto) {
        this.fanLevel = fanLevel;
        this.tempLeft = tempLeft;
        this.tempRight = tempRight;
        this.fanDirection = fanDirection;
        this.ac = ac;
        this.auto = auto;
    }


    @Override
    public String toString() {
        return "AdapterState{" +
                "fanLevel=" + fanLevel +
                ", tempLeft=" + tempLeft +
                ", tempRight=" + tempRight +
                ", fanDirection=" + fanDirection +
                ", ac=" + ac +
                ", auto=" + auto +
                '}';
    }
}
