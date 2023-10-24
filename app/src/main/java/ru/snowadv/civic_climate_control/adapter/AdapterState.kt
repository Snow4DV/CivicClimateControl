package ru.snowadv.civic_climate_control.adapter

import android.content.Context
import com.google.gson.annotations.SerializedName
import ru.snowadv.civic_climate_control.R

data class AdapterState(
    @SerializedName("fanLevel")
    private val fanLevelRaw: Int = 0,
    @SerializedName("tempLeft")
    val tempLeft: Int = 0,
    @SerializedName("tempRight")
    val tempRight: Int = 0,
    @SerializedName("fanDirection")
    private val fanDirectionRaw: Int = 0,
    @SerializedName("ac")
    private val acRaw: Int = 0,
    @SerializedName("auto")
    public val auto: Boolean = false
) {
    enum class ACState (val displayStringId: Int) {
        HIDDEN(R.string.climate_none), ON(R.string.climate_on), OFF(R.string.climate_off)
    }

    enum class FanLevel(val resourceId: Int, val stringValueId: Int) {
        LEVEL_0(R.drawable.ic_fan_speed_0, R.string.fan_0), LEVEL_1(R.drawable.ic_fan_speed_1, R.string.fan_1),
        LEVEL_2(R.drawable.ic_fan_speed_2, R.string.fan_2), LEVEL_3(R.drawable.ic_fan_speed_3, R.string.fan_3),
        LEVEL_4(R.drawable.ic_fan_speed_4, R.string.fan_4), LEVEL_5(R.drawable.ic_fan_speed_5, R.string.fan_5),
        LEVEL_6(R.drawable.ic_fan_speed_6, R.string.fan_6), LEVEL_7(R.drawable.ic_fan_speed_7, R.string.fan_7),
        AUTO(R.drawable.ic_fan_speed_auto, R.string.fan_auto)

    }

    enum class FanDirection(val resourceId: Int, val stringId: Int) {
        NONE(R.drawable.ic_fan_dir_none, R.string.climate_none), UP(R.drawable.ic_fan_dir_up, R.string.fan_up), DOWN(R.drawable.ic_fan_dir_down, R.string.fan_down),
        UP_DOWN(R.drawable.ic_fan_dir_up_down, R.string.fan_up_and_down), DOWN_WINDSHIELD(R.drawable.ic_fan_dir_down_windshield, R.string.fan_down_windshield),
        WINDSHIELD(R.drawable.ic_fan_dir_windshield, R.string.fan_windshield), UP_UPPER(R.drawable.ic_fan_dir_up_upper, R.string.fan_up_upper),
        DOWN_UPPER(R.drawable.ic_fan_dir_down_upper, R.string.fan_down_upper), UP_DOWN_UPPER(R.drawable.ic_fan_dir_up_down_upper, R.string.fan_up_down_upper),
        UPPER(R.drawable.ic_fan_dir_upper, R.string.fan_upper), AUTO(R.drawable.ic_fan_dir_auto, R.string.fan_dir_auto)
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
            6 -> FanDirection.UPPER
            7 -> FanDirection.DOWN_UPPER
            8 -> FanDirection.UP_UPPER
            9 -> FanDirection.UP_DOWN_UPPER
            10 -> FanDirection.AUTO
            else -> FanDirection.NONE
        }
    val fanLevel: FanLevel
        get() = when (fanLevelRaw) {
            -1 -> FanLevel.AUTO
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

    fun toDisplayString(context: Context, includeMode: Boolean = true): String {
        val state = listOfNotNull(
            if (tempLeftVisibility) tempLeftString else null,
            if (includeMode) ("${context.getString(R.string.mode)}: ${context.getString(fanDirection.stringId)}") else null,
            if (fanLevel != FanLevel.LEVEL_0) "${context.getString(R.string.fan)}: ${context.getString(fanLevel.stringValueId)}" else null,
            if (tempRightVisibility) tempRightString else null,
            if (auto) R.string.climate_auto else null
        )
        return state.joinToString(" | ")
    }
}