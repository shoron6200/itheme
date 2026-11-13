package com.shoron.mono.media

import android.service.notification.NotificationListenerService

/**
 * Android only lets an app see other apps' media sessions (MediaSessionManager.getActiveSessions)
 * if the user has granted it notification access, which requires declaring a
 * NotificationListenerService. MONO's listener is intentionally empty: it never reads,
 * stores or acts on notifications, and it does not play or keep any media alive.
 * The system binds it only while access is granted.
 */
class MediaAccessService : NotificationListenerService()
