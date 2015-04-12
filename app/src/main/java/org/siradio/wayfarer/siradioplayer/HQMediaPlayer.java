package org.siradio.wayfarer.siradioplayer;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.PowerManager;
import android.util.Log;

import java.io.IOException;

/**
 * Created by wayfarer on 3/30/15.
 */
public class HQMediaPlayer implements SiMediaPlayer {
    private static final String URL = "http://listen.siradio.fm";
    private static final String LOG_TAG = "HQMediaPlayer";
    private MediaPlayer hqMediaPlayer = null;

    public void init(SiRadioPlayerService radioPlayerService) {
        if (hqMediaPlayer == null) {
            hqMediaPlayer = new MediaPlayer();
        }

        HQRadioPlayerService rps = (HQRadioPlayerService)radioPlayerService;
        Context applicationContext = rps.getApplicationContext();
        hqMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        hqMediaPlayer.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK);
        hqMediaPlayer.setOnPreparedListener(rps);
        hqMediaPlayer.setOnErrorListener(rps);

        try {
            hqMediaPlayer.setDataSource(URL);
        } catch (IOException e) {
            hqMediaPlayer = null;
            Log.e(LOG_TAG, e.getMessage(), e);
            return;
        }

        hqMediaPlayer.prepareAsync(); // prepare async to not block main thread
    }

    public void destroy() {
        if (hqMediaPlayer != null) {
            hqMediaPlayer.stop();
            hqMediaPlayer.release();
            hqMediaPlayer = null;
        }
    }

    public void start(SiRadioPlayerService hqRadioPlayerService) {
        if (hqMediaPlayer != null) {
            hqMediaPlayer.prepareAsync();
        } else {
            init(hqRadioPlayerService);
        }
    }

    public boolean stop() {
        if (hqMediaPlayer != null) {
            hqMediaPlayer.stop();
            return true;
        }

        return false;
    }

    @Override
    public String getURL() {
        return URL;
    }

}
