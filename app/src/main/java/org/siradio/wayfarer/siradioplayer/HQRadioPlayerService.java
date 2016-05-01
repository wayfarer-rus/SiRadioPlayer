package org.siradio.wayfarer.siradioplayer;

import android.media.MediaPlayer;
import android.util.Log;

/**
 * Project SIRadioStreamPlayer
 * Created by wayfarer on 3/29/15.
 */
public class HQRadioPlayerService extends SiRadioPlayerService implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener
{
    private static final String LOG_TAG = "HQRadioPlayerService";

    public HQRadioPlayerService() {
        super(new HQMediaPlayer(), LQRadioPlayerService.class, LOG_TAG);
    }

    @Override
    public void onPrepared(MediaPlayer player) {
        Log.d(LOG_TAG, "HQMediaPlayer prepared. Other player will be destroyed.");
        sendMessageToOtherRadioService(SiRadioPlayerService.DESTROY);
        player.start();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(LOG_TAG, "Error in HQMediaPlayer: " + what + "; " + extra);
        Log.e(LOG_TAG, "HQMediaPlayer will be destroyed");
        mediaPlayer.destroy();
        sendMessageToMasterController(MasterRadioControllerService.RESTART_PLAYER);
        // Error handled
        return true;
    }
}
