package ru.snowadv.civic_climate_control.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import ru.snowadv.civic_climate_control.R
import java.lang.Exception

class AppListDialog(private val context: Context) {

    class ListApplication(val packageName: String, val displayName: String, val iconResourceId: Int) {
        override fun toString(): String = displayName
    }
    fun show() {
        val appList = getInstalledApps()
        val appNames = appList.filter { it.name != null }
            .map { ListApplication(it.packageName, it.nonLocalizedLabel?.toString() ?: it.packageName, it.icon) }.toTypedArray()

        val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, appNames)
        val listView = ListView(context)
        listView.adapter = adapter

        val alertDialogBuilder = AlertDialog.Builder(context)
        alertDialogBuilder.setTitle(context.getString(R.string.run_app))
        alertDialogBuilder.setView(listView)

        val dialog = alertDialogBuilder.create()

        listView.setOnItemClickListener { _, _, position, _ ->
            val packageName = appList[position].packageName
            openApp(packageName)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun getInstalledApps(): List<android.content.pm.ApplicationInfo> {
        val packageManager = context.packageManager
        return packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
    }

    private fun openApp(packageName: String) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            context.startActivity(launchIntent)
        }
    }

    private fun Context.getStringOrNull(id: Int): String? {
        return try {
            getString(id)
        } catch(ex: Exception) {
            null
        }
    }
}
