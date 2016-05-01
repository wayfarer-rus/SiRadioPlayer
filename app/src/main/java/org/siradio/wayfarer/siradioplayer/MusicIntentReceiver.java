package org.siradio.wayfarer.siradioplayer;

import android.content.*;
import android.support.v4.content.*;

public class MusicIntentReceiver extends android.content.BroadcastReceiver
{
    public static final String AUDIO_BECOMES_NOISY = "org.siradio.wayfarer.siradioplayer.MusicIntentReceiver.AudioBecomingNoisy";
    @Override
    public void onReceive(Context ctx, Intent intent)
    {
        if (intent.getAction().equals(
                android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY))
        {
            Intent audioBecomesNoisy = new Intent(AUDIO_BECOMES_NOISY);
            LocalBroadcastManager.getInstance(ctx).sendBroadcast(audioBecomesNoisy);
        }
    }
}
