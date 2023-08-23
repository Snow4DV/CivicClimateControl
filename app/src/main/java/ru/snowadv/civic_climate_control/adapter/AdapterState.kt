package ru.snowadv.civic_climate_control.adapter

import ru.snowadv.civic_climate_control.R

data class AdapterState(
    private val fanLevelRaw: Int = 0,
    val tempLeft: Int = 0,
    val tempRight: Int = 0,
    private val fanDirectionRaw: Int = 0,
    private val acRaw: Int = 0,
    private val autoRaw: Boolean = false
) {
    enum class ACState {
        HIDDEN, ON, OFF
    }

    enum class FanLevel(val resourceId: Int) {
        LEVEL_0(R.drawable.ic_fan_speed_0), LEVEL_1(R.drawable.ic_fan_speed_1),
        LEVEL_2(R.drawable.ic_fan_speed_2), LEVEL_3(R.drawable.ic_fan_speed_3),
        LEVEL_4(R.drawable.ic_fan_speed_4), LEVEL_5(R.drawable.ic_fan_speed_5),
        LEVEL_6(R.drawable.ic_fan_speed_6), LEVEL_7(R.drawable.ic_fan_speed_7);

    }

    enum class FanDirection(val resourceId: Int) {
        NONE(R.drawable.ic_fan_dir_none), UP(R.drawable.ic_fan_dir_up), DOWN(R.drawable.ic_fan_dir_down),
        UP_DOWN(R.drawable.ic_fan_dir_up_down), DOWN_WINDSHIELD(R.drawable.ic_fan_dir_down_windshield),
        WINDSHIELD(R.drawable.ic_fan_dir_windshield);

    }

    val acState: ACState
        get() = when (acRaw) {
            0 -> ACState.HIDDEN
            1 -> ACState.ON
            2 -> ACState.OFF
            else -> ACState.HIDDEN
        }

    val fanDirection: FanDirection
        get() = when (fanDirectionRaw) {
            0 -> FanDirection.NONE
            1 -> FanDirection.UP
            2 -> FanDirection.UP_DOWN
            3 -> FanDirection.DOWN
            4 -> FanDirection.DOWN_WINDSHIELD
            5 -> FanDirection.WINDSHIELD
            else -> FanDirection.NONE
        }
    val fanLevel: FanLevel
        get() = when (fanLevelRaw) {
            0 -> FanLevel.LEVEL_0
            1 -> FanLevel.LEVEL_1
            2 -> FanLevel.LEVEL_2
            3 -> FanLevel.LEVEL_3
            4 -> FanLevel.LEVEL_4
            5 -> FanLevel.LEVEL_5
            6 -> FanLevel.LEVEL_6
            7 -> FanLevel.LEVEL_7
            else -> FanLevel.LEVEL_0
        }

    val tempLeftString: String
        get() = when (tempLeft) {
            -1 -> ""
            0 -> "LO"
            99 -> "HI"
            else -> tempLeft.toString()
        }
    val tempRightString: String
        get() = when (tempRight) {
            -1 -> ""
            0 -> "LO"
            99 -> "HI"
            else -> tempRight.toString()
        }
    val tempRightVisibility: Boolean
        get() = tempRight != -1 && tempLeft != 78 && tempLeft != 79 // adapter returns it when connection to climate unit is lost
    val tempLeftVisibility: Boolean
        get() = tempLeft != -1 && tempLeft != 78 && tempLeft != 79
}