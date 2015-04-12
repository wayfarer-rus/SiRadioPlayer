package org.siradio.wayfarer.siradioplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

/**
 * Project SiRadioPlayer
 * Created by wayfarer on 4/4/15.
 */
public class NetworkReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = "NetworkReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager conn = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = conn.getActiveNetworkInfo();
        MasterRadioControllerService service = (MasterRadioControllerService)context;
        Message message;

        // Checks the network connection. Based on the result, decides whether
        // to change player quality or keep it
        if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            // If device has its Wi-Fi connection, sets PLAYER_QUALITY
            // to HQ. Then update player
            message = Message.obtain(null, MasterRadioControllerService.WIFI_AVAILABLE, 0, 0);
            Log.d(LOG_TAG, "WiFi Connection Detected");

            // If there is a mobile network connection, sets PLAYER_QUALITY to LQ.
            // Then update player
        } else if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
            message = Message.obtain(null, MasterRadioControllerService.MOBILE_AVAILABLE, 0, 0);
            Log.d(LOG_TAG, "Mobile Connection Detected");

            // Otherwise, the app can't download content-- because there is no network
            // connection (mobile or Wi-Fi),
            // Sets PLAYER_QUALITY to NONE.
        } else {
            message = Message.obtain(null, MasterRadioControllerService.NETWORK_DOWN, 0, 0);
            Log.d(LOG_TAG, "Connection Lost");
        }

        if (message != null) {
            try {
                service.mServiceHandler.send(message);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
            }
        }
    }
}