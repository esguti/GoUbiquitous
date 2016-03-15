package com.portfolio.course.esguti.goubiquitous.mobile;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by esguti on 14.03.16.
 */
public class ListenerService extends WearableListenerService {
    private static final String LOG_TAG = ListenerService.class.getSimpleName();

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        final String message = new String(messageEvent.getData());

        Log.d(LOG_TAG, "Message path received on watch is: " + messageEvent.getPath());
        Log.d(LOG_TAG, "Message received on watch is: " + message);

        // Broadcast message to wearable activity for display
        Intent messageIntent = new Intent();
        messageIntent.setAction(Intent.ACTION_SEND);
        messageIntent.putExtra(getString(R.string.message_path), messageEvent.getPath());
        messageIntent.putExtra(getString(R.string.message_data), message);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(messageIntent);
    }
}
