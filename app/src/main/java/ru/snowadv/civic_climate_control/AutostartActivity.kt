package ru.snowadv.civic_climate_control

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import ru.snowadv.civic_climate_control.overlay.LayoutClimateOverlay

private const val TAG = "AutostartOverlActivity"

class AutostartActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val climateAutoStart = ClimateAutoStart()
        climateAutoStart.onReceive(this, this.intent)
        overridePendingTransition(0, 0)
        finish()
        super.onCreate(savedInstanceState)
    }
}