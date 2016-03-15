package com.portfolio.course.esguti.goubiquitous.mobile.sync;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

public class SunshineSyncService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String LOG_TAG = SunshineSyncService.class.getSimpleName();

    private static final Object sSyncAdapterLock = new Object();
    private static SunshineSyncAdapter sSunshineSyncAdapter = null;

    //for connecting to Wearable
    private GoogleApiClient m_apiClient;


    @Override
    public void onCreate() {
        Log.d("SunshineSyncService", "onCreate - SunshineSyncService");

        //initialize Wearable connection
        m_apiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                // Request access only to the Wearable API
                .addApi(Wearable.API)
                .build();

        m_apiClient.connect();

        synchronized (sSyncAdapterLock) {
            if (sSunshineSyncAdapter == null) {
                sSunshineSyncAdapter = new SunshineSyncAdapter(getApplicationContext(), true, m_apiClient);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sSunshineSyncAdapter.getSyncAdapterBinder();
    }

//    public void onDestroy(){
//        if( m_apiClient != null) m_apiClient.disconnect();
//    }

    private void sendMessage( final String path, final String text ) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(m_apiClient).await();
                for (Node node : nodes.getNodes()) {
                    Log.d(LOG_TAG, "Sending initial message");
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            m_apiClient, node.getId(), path, text.getBytes()).await();
                }

            }
        }).start();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(LOG_TAG, "Hello wearable");
        String message = "Hello wearable\n Via the data layer";
        sendMessage("/message_path", message);
    }

    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {}
}