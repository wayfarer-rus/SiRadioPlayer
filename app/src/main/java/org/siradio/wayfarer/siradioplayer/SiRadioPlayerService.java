package org.siradio.wayfarer.siradioplayer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.widget.Toast;

/**
 * Project SIRadioStreamPlayer
 * Created by wayfarer on 3/29/15.
 */
public class SiRadioPlayerService extends Service {
    public static final String MEDIA_INFO_ACTION = "org.siradio.wayfarer.siradioplayer.SiRadioPlayerService.MediaInfo";
    private String logTag = "SiRadioPlayerService";

    public static final int DESTROY = 99;
    public static final int STOP = 2;
    public static final int START = 1;

    final Messenger mServiceHandler  = new Messenger(new SiRadioPlayerServiceHandler());

    protected SiMediaPlayer mediaPlayer;
    private Class otherPlayerServiceClass;

    private SiPlayerServiceConnection mOtherPlayerConnection = new SiPlayerServiceConnection();
    private SiPlayerServiceConnection mMasterConnection = new SiPlayerServiceConnection();
    private SiPlayerServiceConnection mMediaInfoConnection = new SiPlayerServiceConnection();

    protected SiRadioPlayerService (SiMediaPlayer mp, Class mps, String name) {
        super();
        mediaPlayer = mp;
        otherPlayerServiceClass = mps;
        logTag = name;
    }

    @Override
    public void onCreate() {
        Log.d(logTag, "service created");
        doBindServices();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return false;
    }

    private void doUnbindServices() {
        if (mOtherPlayerConnection.isServiceBound()) {
            unbindService(mOtherPlayerConnection);
            mOtherPlayerConnection.setServiceBound(false);
        }

        if (mMasterConnection.isServiceBound()) {
            unbindService(mMasterConnection);
            mMasterConnection.setServiceBound(false);
        }

        if (mMediaInfoConnection.isServiceBound()) {
            unbindService(mMediaInfoConnection);
            mMediaInfoConnection.setServiceBound(false);
        }
    }
    
    @Override
    public void onDestroy() {
        doUnbindServices();
        mediaPlayer.destroy();
        Log.d(logTag, "service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mServiceHandler.getBinder();
    }

    private void doBindServices() {
        bindService(new Intent(this, otherPlayerServiceClass), mOtherPlayerConnection, Context.BIND_AUTO_CREATE);
        bindService(new Intent(this, MasterRadioControllerService.class), mMasterConnection, Context.BIND_AUTO_CREATE);
        bindService(new Intent(this, MediaInfoService.class), mMediaInfoConnection, Context.BIND_AUTO_CREATE);
    }

    protected void sendMessageToOtherRadioService(int msgId) {
        mOtherPlayerConnection.sendMessageToService(msgId);
    }

    protected void sendMessageToMasterController(int msgId) {
        mMasterConnection.sendMessageToService(msgId);
    }

    private void start() {
        Toast.makeText(this, "Split Infinity player starting", Toast.LENGTH_SHORT).show();
        mMediaInfoConnection.sendMessageToService(MediaInfoService.START_COLLECTING_INFO);
        mediaPlayer.start(this);
    }

    private void stop() {
        if (mediaPlayer.stop()) {
            Toast.makeText(this, "Split Infinity player stopped", Toast.LENGTH_SHORT).show();
        }

        mMediaInfoConnection.sendMessageToService(MediaInfoService.STOP_COLLECTING_INFO);
    }

    // Handler that receives messages from the thread
    private final class SiRadioPlayerServiceHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DESTROY:
                    Log.d(logTag, "Player will be destroyed");
                    mediaPlayer.destroy();
                    break;
                case START:
                    Log.d(logTag, "Player will be started");
                    start();
                    break;
                case STOP:
                    Log.d(logTag, "Player will be stopped");
                    stop();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
