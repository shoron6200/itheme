package com.shoron.mono.launcher.quick

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings

/** System shortcuts used by the swipe-down panel. None need runtime permissions. */
object QuickActions {

    fun openInternet(context: Context) = start(
        context,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
        else Intent(Settings.ACTION_WIRELESS_SETTINGS),
    )

    fun openBluetooth(context: Context) = start(context, Intent(Settings.ACTION_BLUETOOTH_SETTINGS))

    fun openCamera(context: Context) = start(context, Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))

    fun openSettings(context: Context) = start(context, Intent(Settings.ACTION_SETTINGS))

    /** Camera with a flash unit, if any (torch control needs no permission). */
    fun torchCameraId(context: Context): String? {
        val cm = context.getSystemService(CameraManager::class.java) ?: return null
        return runCatching {
            cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull()
    }

    fun setTorch(context: Context, on: Boolean): Boolean {
        val id = torchCameraId(context) ?: return false
        return runCatching { context.getSystemService(CameraManager::class.java)?.setTorchMode(id, on) }.isSuccess
    }

    /**
     * Pulls down the notification shade. Android offers no public API for this; launchers use
     * StatusBarManager.expandNotificationsPanel (permission EXPAND_STATUS_BAR, normal). Returns
     * false if the ROM blocks it, so the caller can fall back to the quick panel.
     */
    @SuppressLint("WrongConstant")
    fun expandNotifications(context: Context): Boolean = runCatching {
        val service = context.getSystemService("statusbar") ?: return false
        Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel").invoke(service)
    }.isSuccess

    private fun start(context: Context, intent: Intent): Boolean = runCatching {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess
}
