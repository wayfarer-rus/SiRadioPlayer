package org.siradio.wayfarer.siradioplayer;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

/**
 * Project SiRadioPlayer
 * Created by wayfarer on 4/10/15.
 */
public class SiPlayerServiceConnection implements ServiceConnection {
    private static final String LOG_TAG = "SiPlServiceConnection";
    private Messenger mServiceMessenger;
    private boolean mServiceBound;

    public void onServiceConnected(ComponentName className, IBinder service) {
        mServiceMessenger = new Messenger(service);
        mServiceBound = true;
        Log.d(LOG_TAG, className.getClassName() + " bounded");
    }

    public void onServiceDisconnected(ComponentName className) {
        mServiceMessenger = null;
        mServiceBound = false;
        Log.d(LOG_TAG, className.getClassName() + " unbounded");
    }


    public Messenger getServiceMessenger() {
        return mServiceMessenger;
    }

    public boolean isServiceBound() {
        return mServiceBound;
    }

    public void setServiceBound(boolean isBound) {
        mServiceBound = isBound;
    }

    public void sendMessageToService(int msgId) {
        if (!mServiceBound) return;
        Message msg = Message.obtain(null, msgId, 0, 0);

        try {
            mServiceMessenger.send(msg);
        } catch (RemoteException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }
    }
}
