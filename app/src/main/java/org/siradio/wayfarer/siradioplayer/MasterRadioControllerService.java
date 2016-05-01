package org.siradio.wayfarer.siradioplayer;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/**
 * Project SiRadioPlayer
 * Created by wayfarer on 4/4/15.
 */
public class MasterRadioControllerService extends Service implements AudioManager.OnAudioFocusChangeListener
{
    private static final String LOG_TAG = "MasterRadioController";
    public static final int DESTROY = 99;
    public static final int PLAY = 1;
    public static final int STOP = 2;
    public static final int WIFI_AVAILABLE = 3;
    public static final int MOBILE_AVAILABLE = 4;
    public static final int NETWORK_DOWN = 5;
    public static final int RESTART_PLAYER = 6;
    public static final String PLAYER_STATUS = "org.siradio.wayfarer.siradioplayer.MasterRadioControllerService.PlayerStatus";
    private PLAYER_STATE_ENUM state = PLAYER_STATE_ENUM.STOP;
    private boolean connectivityFlagsNotSet = true;

    public static enum PLAYER_QUALITY_ENUM
    {
        NONE, HQ, LQ
        }

    private static enum PLAYER_STATE_ENUM
    {
        NONE, PLAY, STOP, AUDIO_FOCUS_LOST
        }

    // Current player quality.
    private PLAYER_QUALITY_ENUM playerQuality = PLAYER_QUALITY_ENUM.NONE;

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mServiceHandler = new Messenger(new MasterRadioControllerServiceHandler());

    // The BroadcastReceiver that tracks network connectivity changes.
    private NetworkReceiver networkReceiver = new NetworkReceiver();

    // Connections to player services
    private SiPlayerServiceConnection mLqConnection = new SiPlayerServiceConnection();
    private SiPlayerServiceConnection mHqConnection = new SiPlayerServiceConnection();

    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_STOP = "action_stop";

    private MediaSession mSession;
    private MediaController mController;

    @Override
    public void onCreate()
    {
        Log.d(LOG_TAG, "Service starting");
        // Setup an AudioFocusListener
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                                                    AudioManager.AUDIOFOCUS_GAIN);

        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        {
            Log.e(LOG_TAG, "could not get audio focus.");
        }
        
        updateConnectedFlags();
        doBindPlayers();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (mSession == null)
        {
            initMediaSessions();
        }

        handleIntent(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    private void initMediaSessions()
    {
        mSession = new MediaSession(getApplicationContext(), "split infinity player session");
        mController = new MediaController(getApplicationContext(), mSession.getSessionToken());

        mSession.setCallback(new MediaSession.Callback(){
                @Override
                public void onPlay()
                {
                    Log.d("MediaPlayerService", "onPlay");
                    state = PLAYER_STATE_ENUM.PLAY;
                    handlePlay();
                }

                @Override
                public void onPause()
                {
                    Log.d("MediaPlayerService", "onPause");
                }

                @Override
                public void onStop()
                {
                    Log.d("MediaPlayerService", "onStop");
                    state = PLAYER_STATE_ENUM.STOP;
                    handleStop();
                }
            }
        );
    }

    @Override
    public void onDestroy()
    {
        doUnbindPlayers();
        // Unregisters BroadcastReceiver when app is destroyed.
        if (networkReceiver != null)
        {
            this.unregisterReceiver(networkReceiver);
        }

        Log.d(LOG_TAG, "Service destroyed");
    }

    private void handleIntent(Intent intent)
    {
        if (intent == null || intent.getAction() == null)
            return;

        String action = intent.getAction();

        if (action.equalsIgnoreCase(ACTION_PLAY))
        {
            mController.getTransportControls().play();
        }
        else if (action.equalsIgnoreCase(ACTION_PAUSE))
        {
            mController.getTransportControls().pause();
        }
        else if (action.equalsIgnoreCase(ACTION_STOP))
        {
            mController.getTransportControls().stop();
        }
    }

    // Checks the network connection and sets the wifiConnected and mobileConnected
    // variables accordingly.
    private void updateConnectedFlags()
    {
        Log.d(LOG_TAG, "+ enter updateConnectedFlags()");

        if (connectivityFlagsNotSet == false) {
            Log.d(LOG_TAG, "- leave updateConnectedFlags: connectivity flags already set");
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
            playerQuality = PLAYER_QUALITY_ENUM.NONE;
            connectivityFlagsNotSet = true;
            Log.d(LOG_TAG, "- leave updateConnectedFlags: ACCESS_NETWORK_STATE permission is NOT granted");
            return;
        }

        // Registers BroadcastReceiver to track network connection changes.
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        networkReceiver = new NetworkReceiver();
        this.registerReceiver(networkReceiver, filter);

        ConnectivityManager connMgr = (ConnectivityManager)
            getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
        boolean wifiConnected;
        boolean mobileConnected;

        if (activeInfo != null && activeInfo.isConnected())
        {
            wifiConnected = activeInfo.getType() == ConnectivityManager.TYPE_WIFI;
            mobileConnected = activeInfo.getType() == ConnectivityManager.TYPE_MOBILE;
        }
        else
        {
            wifiConnected = false;
            mobileConnected = false;
        }

        if (wifiConnected)
        {
            playerQuality = PLAYER_QUALITY_ENUM.HQ;
        }
        else if (mobileConnected)
        {
            playerQuality = PLAYER_QUALITY_ENUM.LQ;
        }
        else
        {
            playerQuality = PLAYER_QUALITY_ENUM.NONE;
        }

        Log.d(LOG_TAG, "- leave updateConnectedFlags: success");
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        Log.d(LOG_TAG, "Service started");
        return mServiceHandler.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        mSession.release();
        mSession = null;
        return false;
    }

    private void doUnbindPlayers()
    {
        if (mLqConnection.isServiceBound())
        {
            mLqConnection.sendMessageToService(LQRadioPlayerService.DESTROY);
            unbindService(mLqConnection);
            mLqConnection.setServiceBound(false);
        }

        if (mHqConnection.isServiceBound())
        {
            mHqConnection.sendMessageToService(HQRadioPlayerService.DESTROY);
            unbindService(mHqConnection);
            mHqConnection.setServiceBound(false);
        }
    }

    private void doBindPlayers()
    {
        Intent intentLq = new Intent(this, LQRadioPlayerService.class);
        Intent intentHq = new Intent(this, HQRadioPlayerService.class);
        // Bind to the services
        bindService(intentLq, mLqConnection, Context.BIND_AUTO_CREATE);
        bindService(intentHq, mHqConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onAudioFocusChange(int focusChange)
    {
        switch (focusChange)
        {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (state == PLAYER_STATE_ENUM.AUDIO_FOCUS_LOST)
                {
                    state = PLAYER_STATE_ENUM.PLAY;
                    handlePlay();
                }
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                state = PLAYER_STATE_ENUM.STOP;
                handleDestroy();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                state = PLAYER_STATE_ENUM.AUDIO_FOCUS_LOST;
                handleStop();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                state = PLAYER_STATE_ENUM.AUDIO_FOCUS_LOST;
                handleStop();
                break;
        }
    }

    private void handleDestroy()
    {
        mHqConnection.sendMessageToService(HQRadioPlayerService.DESTROY);
        mLqConnection.sendMessageToService(LQRadioPlayerService.DESTROY);
        // Broadcast that player is stopped
        Intent playerStoppedIntent = new Intent(MasterRadioControllerService.PLAYER_STATUS);
        playerStoppedIntent.putExtra("status", "stopped");
        LocalBroadcastManager.getInstance(this).sendBroadcast(playerStoppedIntent);
    }

    private void handleStop()
    {
        mHqConnection.sendMessageToService(HQRadioPlayerService.STOP);
        mLqConnection.sendMessageToService(LQRadioPlayerService.STOP);
        // Broadcast that player is stopped
        Intent playerStoppedIntent = new Intent(MasterRadioControllerService.PLAYER_STATUS);
        playerStoppedIntent.putExtra("status", "stopped");
        LocalBroadcastManager.getInstance(this).sendBroadcast(playerStoppedIntent);
    }

    private void handlePlay()
    {
        updateConnectedFlags();

        switch (playerQuality)
        {
            case HQ:
                {
                    Log.d(LOG_TAG, "HQ player will be called");
                    mHqConnection.sendMessageToService(HQRadioPlayerService.START);
                    // Broadcast that player is started
                    Intent playerStoppedIntent = new Intent(MasterRadioControllerService.PLAYER_STATUS);
                    playerStoppedIntent.putExtra("status", "started");
                    playerStoppedIntent.putExtra("quality", "HQ");
                    LocalBroadcastManager.getInstance(this).sendBroadcast(playerStoppedIntent);
                }
                break;
            case LQ:
                {
                    Log.d(LOG_TAG, "LQ player will be called");
                    mLqConnection.sendMessageToService(LQRadioPlayerService.START);
                    // Broadcast that player is started
                    Intent playerStoppedIntent = new Intent(MasterRadioControllerService.PLAYER_STATUS);
                    playerStoppedIntent.putExtra("status", "started");
                    playerStoppedIntent.putExtra("quality", "LQ");
                    LocalBroadcastManager.getInstance(this).sendBroadcast(playerStoppedIntent);
                }
                break;
            default:
                Log.d(LOG_TAG, "No player will be called. Connection is down");
                // Broadcast that player is stopped
                Intent playerStoppedIntent = new Intent(MasterRadioControllerService.PLAYER_STATUS);
                playerStoppedIntent.putExtra("status", "stopped");
                LocalBroadcastManager.getInstance(this).sendBroadcast(playerStoppedIntent);
                break;    
        }
    }

    // Handler that receives messages from the thread
    private final class MasterRadioControllerServiceHandler extends Handler
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case DESTROY:
                    Log.d(LOG_TAG, "Service will be destroyed soon");
                    MasterRadioControllerService.this.stopSelf();
                    break;
                case PLAY:
                    state = PLAYER_STATE_ENUM.PLAY;
                    handlePlay();
                    break;
                case STOP:
                    state = PLAYER_STATE_ENUM.STOP;
                    handleStop();
                    break;
                case WIFI_AVAILABLE:
                    if (playerQuality != PLAYER_QUALITY_ENUM.HQ)
                    {
                        playerQuality = PLAYER_QUALITY_ENUM.HQ;

                        if (state != PLAYER_STATE_ENUM.STOP
                            && state != PLAYER_STATE_ENUM.AUDIO_FOCUS_LOST)
                        {
                            handlePlay();
                        }
                    }

                    break;
                case MOBILE_AVAILABLE:
                    if (playerQuality != PLAYER_QUALITY_ENUM.LQ)
                    {
                        playerQuality = PLAYER_QUALITY_ENUM.LQ;

                        if (state != PLAYER_STATE_ENUM.STOP
                            && state != PLAYER_STATE_ENUM.AUDIO_FOCUS_LOST)
                        {
                            handlePlay();
                        }
                    }

                    break;
                case NETWORK_DOWN:
                    if (playerQuality != PLAYER_QUALITY_ENUM.NONE)
                    {
                        playerQuality = PLAYER_QUALITY_ENUM.NONE;
                        handleDestroy();
                    }
                    break;
                case RESTART_PLAYER:
                    if (playerQuality != PLAYER_QUALITY_ENUM.NONE
                        && state != PLAYER_STATE_ENUM.STOP
                        && state != PLAYER_STATE_ENUM.AUDIO_FOCUS_LOST)
                    {
                        handleDestroy();
                        handlePlay();
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

}
