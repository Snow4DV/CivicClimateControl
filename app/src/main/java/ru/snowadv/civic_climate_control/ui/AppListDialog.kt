package ru.snowadv.civic_climate_control.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.startActivity
import ru.snowadv.civic_climate_control.R


class AppListDialog(private val context: Context) {

    class ListApplication(val displayName: CharSequence, val iconResourceId: Int, resolveInfo: ResolveInfo) {
        override fun toString(): String = displayName.toString()
    }
    fun show() {
        val appList = getInstalledApps()
        val appNames = appList
            .map {
                ListApplication(it.loadLabel(context.packageManager), it.iconResource, it)
            }.toTypedArray()

        val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, appNames)
        val listView = ListView(context)
        listView.adapter = adapter

        val alertDialogBuilder = AlertDialog.Builder(context)
        alertDialogBuilder.setTitle(context.getString(R.string.run_app))
        alertDialogBuilder.setView(listView)

        val dialog = alertDialogBuilder.create()

        listView.setOnItemClickListener { _, _, position, _ ->
            openApp(appList[position])
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun getInstalledApps(): List<ResolveInfo> {
        val packageManager = context.packageManager
        return packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN, null), 0)
            .sortedWith(ResolveInfo.DisplayNameComparator(context.packageManager))
    }

    private fun openApp(launchable: ResolveInfo) {
        val activity = launchable.activityInfo
        val name = ComponentName(
            activity.applicationInfo.packageName,
            activity.name
        )
        val i = Intent(Intent.ACTION_MAIN)

        i.addCategory(Intent.CATEGORY_LAUNCHER)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        i.component = name

        startActivity(context, i, null)
    }

    private fun Context.getStringOrNull(id: Int): String? {
        return try {
            getString(id)
        } catch(ex: Exception) {
            null
        }
    }
}
