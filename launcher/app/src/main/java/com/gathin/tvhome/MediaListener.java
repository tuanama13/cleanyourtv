package com.gathin.tvhome;

import android.service.notification.NotificationListenerService;

/**
 * Does nothing itself: an enabled notification listener is what lets an app read other apps' media sessions
 * (MediaSessionManager.getActiveSessions), which the Home media card needs. Enable it with
 * cmd notification allow_listener com.gathin.tvhome/com.gathin.tvhome.MediaListener
 */
public final class MediaListener extends NotificationListenerService {
}
