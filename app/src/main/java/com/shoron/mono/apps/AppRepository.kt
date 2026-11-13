package com.shoron.mono.apps

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.text.Collator

/**
 * Source of truth for installed launchable apps, across the main and work profiles.
 *
 * Event driven: the list reloads only when [LauncherApps.Callback] reports a package change.
 * There is no polling.
 */
class AppRepository(
    context: Context,
    scope: CoroutineScope,
    private val iconCache: IconCache,
) : IconLoader {

    private val appContext = context.applicationContext
    private val launcherApps = appContext.getSystemService(LauncherApps::class.java)
    private val userManager = appContext.getSystemService(UserManager::class.java)
    private val densityDpi = appContext.resources.displayMetrics.densityDpi

    /** `null` until the first load completes. */
    val apps: StateFlow<List<AppEntry>?> = callbackFlow {
        val callback = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String, user: UserHandle) {
                iconCache.invalidatePackage(packageName)
                trySend(Unit)
            }

            override fun onPackageAdded(packageName: String, user: UserHandle) {
                trySend(Unit)
            }

            override fun onPackageChanged(packageName: String, user: UserHandle) {
                iconCache.invalidatePackage(packageName)
                trySend(Unit)
            }

            override fun onPackagesAvailable(
                packageNames: Array<out String>,
                user: UserHandle,
                replacing: Boolean,
            ) {
                trySend(Unit)
            }

            override fun onPackagesUnavailable(
                packageNames: Array<out String>,
                user: UserHandle,
                replacing: Boolean,
            ) {
                trySend(Unit)
            }
        }
        trySend(Unit)
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        awaitClose { launcherApps.unregisterCallback(callback) }
    }
        .conflate()
        .map { loadApps() }
        .flowOn(Dispatchers.Default)
        .stateIn(scope, SharingStarted.Eagerly, null)

    private fun loadApps(): List<AppEntry> {
        val self = appContext.packageName
        val mainUser = Process.myUserHandle()
        val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }
        return userManager.userProfiles
            .flatMap { user ->
                val serial = userManager.getSerialNumberForUser(user)
                runCatching { launcherApps.getActivityList(null, user) }
                    .getOrDefault(emptyList())
                    .map { info ->
                        val component = info.componentName
                        AppEntry(
                            key = AppKeys.of(component.packageName, component.className, serial),
                            label = info.label?.toString()?.trim().orEmpty()
                                .ifEmpty { component.packageName },
                            packageName = component.packageName,
                            className = component.className,
                            userSerial = serial,
                            isWorkProfile = user != mainUser,
                        )
                    }
            }
            .filterNot { it.packageName == self }
            .distinctBy { it.key }
            .sortedWith { a, b -> collator.compare(a.label, b.label) }
    }

    override fun peek(entry: AppEntry, sizePx: Int): ImageBitmap? = iconCache.get(entry.key, sizePx)

    override suspend fun load(entry: AppEntry, sizePx: Int): ImageBitmap? {
        iconCache.get(entry.key, sizePx)?.let { return it }
        return withContext(Dispatchers.Default) {
            val user = userManager.getUserForSerialNumber(entry.userSerial) ?: return@withContext null
            val intent = Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(ComponentName(entry.packageName, entry.className))
            val info = runCatching { launcherApps.resolveActivity(intent, user) }.getOrNull()
                ?: return@withContext null
            val drawable = runCatching { info.getBadgedIcon(densityDpi) }.getOrNull()
                ?: return@withContext null
            runCatching { drawable.toBitmap(sizePx, sizePx).asImageBitmap() }
                .getOrNull()
                ?.also { iconCache.put(entry.key, sizePx, it) }
        }
    }
}
