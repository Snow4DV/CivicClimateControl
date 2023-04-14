package ru.snowadv.civic_climate_control.Adapter;

import ru.snowadv.civic_climate_control.R;

public final class AdapterState {
    private FanLevel fanLevel = FanLevel.LEVEL_0;

    private int tempLeft = 0;
    private int tempRight = 0;


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

    private FanDirection fanDirection = FanDirection.NONE;

    public FanDirection getFanDirection() {
        return fanDirection == null ? FanDirection.UP : fanDirection;
    }

    private boolean ac = false;
    private boolean auto = false;

    public FanLevel getFanLevel() {
        return fanLevel == null ? FanLevel.LEVEL_0 : fanLevel;
    }

    public int getTempLeft() {
        return tempLeft;
    }

    public int getTempRight() {
        return tempRight;
    }

    public String getTempLeftString() {
        switch(tempLeft) {
            case 0:
                return "LO";
            case 99:
                return "HI";
            default:
                return String.valueOf(tempLeft);
        }
    }

    public String getTempRightString() {
        switch(tempRight) {
            case 0:
                return "LO";
            case 99:
                return "HI";
            default:
                return String.valueOf(tempRight);
        }
    }

    public boolean isAc() {
        return ac;
    }

    public boolean isAuto() {
        return auto;
    }

    public AdapterState(FanLevel fanLevel, int tempLeft, int tempRight, FanDirection fanDirection, boolean ac, boolean auto) {
        this.fanLevel = fanLevel;
        this.tempLeft = tempLeft;
        this.tempRight = tempRight;
        this.fanDirection = fanDirection;
        this.ac = ac;
        this.auto = auto;
    }


}
