package ru.snowadv.civic_climate_control.Adapter;

import ru.snowadv.civic_climate_control.R;

public final class AdapterState {
    public static final int UNAVAILABLE = -1;


    private final FanLevel fanLevel;

    private final int tempLeft;
    private final int tempRight;


    public enum FanLevel {
        LEVEL_1, LEVEL_2, LEVEL_3, LEVEL_4, LEVEL_5, LEVEL_6, LEVEL_7
    }
    public enum FanDirection {
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

    private final FanDirection fanDirection;

    public FanDirection getFanDirection() {
        return fanDirection;
    }

    private final boolean ac;
    private final boolean auto;

    public FanLevel getFanLevel() {
        return fanLevel;
    }

    public int getTempLeft() {
        return tempLeft;
    }

    public int getTempRight() {
        return tempRight;
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
