package ru.snowadv.civic_climate_control

import android.app.ActionBar.LayoutParams
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.res.Resources
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.gson.JsonSyntaxException
import ru.snowadv.civic_climate_control.adapter.AdapterService
import ru.snowadv.civic_climate_control.adapter.AdapterService.AdapterBinder
import ru.snowadv.civic_climate_control.adapter.AdapterService.OnNewStateReceivedListener
import ru.snowadv.civic_climate_control.adapter.AdapterState
import ru.snowadv.civic_climate_control.databinding.ActivityClimateBinding
import ru.snowadv.civic_climate_control.overlay.LayoutClimateOverlay
import ru.snowadv.civic_climate_control.ui.AppListDialog


class ClimateActivity : AbstractServiceConnectedActivity() {
    override val TAG = "ClimateActivity"
    private var binding: ActivityClimateBinding? = null
    private var notifierUtility: NotifierUtility? = null
    private var rootView: View? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notifierUtility = NotifierUtility(this)
        hideTitleAndNotificationBars()
        rootView = initViewBinding()
        setContentView(rootView)
        initInterfaceListeners()
        restartOverlayServiceIfNeeded()
    }


    private fun setSizeAndMarginOfConstraintLayout() {
        try {
            val width = settingsPreferences.getInt("activity_width", 100)
            val height = settingsPreferences.getInt("activity_height", 100)
            val marginTop = settingsPreferences.getInt("activity_margin_top", 0)
            val marginLeft = settingsPreferences.getInt("activity_margin_left", 0)

            val widthDp = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                width.toFloat(),
                resources.getDisplayMetrics()
            ).toInt();
            val heightDp = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                height.toFloat(),
                resources.getDisplayMetrics()
            ).toInt();
            val marginTopDp = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                marginTop.toFloat(),
                resources.getDisplayMetrics()
            ).toInt();
            val marginLeftDp = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                marginLeft.toFloat(),
                resources.getDisplayMetrics()
            ).toInt();


            val params = FrameLayout.LayoutParams(
                if (width != 100) widthDp else LayoutParams.MATCH_PARENT,
                if (height != 100) heightDp else LayoutParams.MATCH_PARENT
            )
            params.setMargins(marginLeftDp, marginTopDp, 0, 0)
            binding?.root?.layoutParams = params
        } catch(exception: Exception) {
            Log.e(TAG, "setSizeAndMarginOfConstraintLayout: unable to set res", exception)
        }
    }

    override fun onResume() {
        super.onResume()
        changeOverlayServiceState(true)
        setSizeAndMarginOfConstraintLayout()
    }

    override fun onPause() {
        super.onPause()
        changeOverlayServiceState(false)
    }

    private fun initViewBinding(): View {
        binding = ActivityClimateBinding.inflate(
            layoutInflater
        )
        return binding?.root ?: throw IllegalStateException("Binding doesn't have root view")
    }

    private fun restartOverlayServiceIfNeeded() {
        val climateAutoStart = ClimateAutoStart()
        climateAutoStart.onReceive(this, this.intent)
    }

    private fun initInterfaceListeners() {
        binding?.settingsButton?.setOnClickListener { openSettingsActivity() }
        binding?.adapterStatusButton?.setOnClickListener {
            restartServiceDialog(
                serviceConnectionAlive
            )
        }
        binding?.appsButton?.setOnClickListener {
            val appListDialog = AppListDialog(this)
            appListDialog.show()
        }
    }

    private fun restartServiceDialog(isAdapterConnectionAlive: Boolean) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(
            if (isAdapterConnectionAlive) getString(R.string.adapter_connected) else getString(
                R.string.adapter_not_connected
            )
        )
            .setNegativeButton(android.R.string.cancel) { dialog: DialogInterface?, which: Int -> }
        if (!isAdapterConnectionAlive) {
            builder.setPositiveButton(R.string.restart_service_button) { dialog: DialogInterface?, which: Int -> initAdapterService() }
        }
        builder.create().show()
    }

    private fun hideTitleAndNotificationBars() {
        supportActionBar?.hide()
        this.window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
    }

    private fun openSettingsActivity() {
        val settingsIntent = Intent(this@ClimateActivity, SettingsActivity::class.java)
        startActivity(settingsIntent)
    }

    private fun changeOverlayServiceState(isActivityVisible: Boolean) {
        val isOverlayEnabled = PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean("floating_panel_enabled", false)
        if (!isOverlayEnabled) return
        if (!isActivityVisible) {
            LayoutClimateOverlay.start(this) // this will restart service if needed
        }
        LayoutClimateOverlay.setClimateActivityIsVisible(isActivityVisible)
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        super.onServiceConnected(name, service)
        binding?.adapterStatusButton?.setImageResource(R.drawable.ic_connected)
    }

    override fun onServiceDisconnected(name: ComponentName) {
        super.onServiceDisconnected(name)
        binding?.adapterStatusButton?.setImageResource(R.drawable.ic_disconnected)
    }

    override fun onBindingDied(name: ComponentName) {
        super.onBindingDied(name)
        binding?.adapterStatusButton?.setImageResource(R.drawable.ic_disconnected)
    }

    override fun onNewAdapterStateReceived(newState: AdapterState) { // it runs in service's thread
        binding?.let { binding ->
            with(binding) {
                binding.root.post {
                    temp1Background.visibility =
                        if (newState.tempLeftVisibility) View.VISIBLE else View.GONE

                    temp1.visibility =
                        if (newState.tempLeftVisibility) View.VISIBLE else View.GONE
                    temp1.text = newState.tempLeftString

                    temp2Background.visibility =
                        if (newState.tempRightVisibility) View.VISIBLE else View.GONE

                    temp2.visibility =
                        if (newState.tempRightVisibility) View.VISIBLE else View.GONE
                    temp2.text = newState.tempRightString

                    acOnGlyph.visibility =
                        if (newState.acState == AdapterState.ACState.ON) View.VISIBLE else View.GONE

                    acOffGlyph.visibility =
                        if (newState.acState == AdapterState.ACState.OFF) View.VISIBLE else View.GONE

                    autoGlyph.visibility = if (newState.auto) View.VISIBLE else View.GONE

                    fanSpeed.setImageResource(newState.fanLevel.resourceId)

                    fanDirection.setImageResource(newState.fanDirection.resourceId)
                }
            }
        }
    }

    override fun onAdapterDisconnected() {
        super.onAdapterDisconnected()
        binding?.root?.post { binding?.adapterStatusButton?.setImageResource(R.drawable.ic_disconnected) }
    }




}