package org.siradio.wayfarer.siradioplayer;

import android.util.Log;

import com.spoledge.aacplayer.PlayerCallback;

/**
 * Project SIRadioStreamPlayer
 * Created by wayfarer on 3/29/15.
 */
public class LQRadioPlayerService extends SiRadioPlayerService implements PlayerCallback {
    private static final String LOG_TAG = "LQRadioPlayerService";
    private boolean started;

    public LQRadioPlayerService() {
        super(new LQMediaPlayer(), HQRadioPlayerService.class, LOG_TAG);
        started = false;
    }

    @Override
    public void playerStarted() {
        Log.d(LOG_TAG, "player started");
        //sendMessageToRadioService(SiRadioPlayerService.DESTROY);
    }

    @Override
    public void playerPCMFeedBuffer(boolean isPlaying, int audioBufferSizeMs, int audioBufferCapacityMs) {
        if (isPlaying && !started) {
            started = true;
            sendMessageToRadioService(SiRadioPlayerService.DESTROY);
        }

        StringBuffer info = new StringBuffer();
        info.append("LQPlayer status: ").append(isPlaying ? "playing" : "stopped").append("; ");
        info.append("Buffer size = \"").append(audioBufferSizeMs).append("\"; ");
        info.append("Buffer capacity = \"").append(audioBufferCapacityMs).append("\"");
        Log.d(LOG_TAG, info.toString());
    }

    @Override
    public void playerStopped(int perf) {
        Log.d(LOG_TAG, "player stopped");
        started = false;
        // request for restart
        sendMessageToMasterController(MasterRadioControllerService.RESTART_PLAYER);
    }

    @Override
    public void playerException(Throwable t) {
        Log.e(LOG_TAG, t.getMessage(), t);
        mediaPlayer.destroy();
        sendMessageToMasterController(MasterRadioControllerService.RESTART_PLAYER);
    }
}