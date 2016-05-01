package org.siradio.wayfarer.siradioplayer;

import android.*;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import android.support.v4.content.*;
import android.util.*;
import java.io.*;
import java.util.*;
import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

/**
 * Project SiRadioPlayer
 * Created by wayfarer on 4/9/15.
 */
public class MediaInfoService extends Service
{
    private static final String LOG_TAG = "MediaInfoService";
    private static final String SI_RADIO_URL = "http://siradio.fm/";
    private static final long ONE_MINUTE = 60 * 1000;

    public static final int START_COLLECTING_INFO = 0;
    public static final int STOP_COLLECTING_INFO = 1;
    public static final int RESEND_INFO = 3;

    public static final String ON_AIR = "onAir";
    public static final String TITLE_NAME = "titleName";
    public static final String SONG_NAME = "songName";
    public static final String FACT_TEXT = "factText";
    public static final String IMAGE_URL = "imageUrl";

    final Messenger mServiceHandler = new Messenger(new MediaInfoServiceHandler());
    private Timer timer;
    private TimerTask timerTask;

    @Override
    public void onCreate()
    {
        Log.d(LOG_TAG, "service created");
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        Log.d(LOG_TAG, "service binded");
        return mServiceHandler.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        Log.d(LOG_TAG, "service unbinded");
        return true;
    }

    @Override
    public void onRebind(Intent intent)
    {
        new HandleMediaInfoAsync().execute();
        Log.d(LOG_TAG, "service REbinded");
    }

    @Override
    public void onDestroy()
    {
        freeTimer();
        Log.d(LOG_TAG, "service destroyed");
    }

    private void freeTimer()
    {
        if (timer != null)
        {
            timer.cancel();
            timer.purge();
            timer = null;
        }

        if (timerTask != null)
        {
            timerTask.cancel();
            timerTask = null;
        }
    }

    private void handleMediaInfo(Connection connection) throws IOException
    {
        Log.d(LOG_TAG, "+ enter handleMediaInfo");
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED)
        {
            Log.d(LOG_TAG, "- leave handleMediaInfo: no internet permissions");
            return;
        }
        
        Document doc = connection.get();
        Log.d(LOG_TAG, "broadcasting message");
        Intent mediaInfoIntent = new Intent(SiRadioPlayerService.MEDIA_INFO_ACTION);
        mediaInfoIntent.putExtra(ON_AIR, getProperty(doc, "npdjonair"));
        mediaInfoIntent.putExtra(TITLE_NAME, getProperty(doc, "nptrack"));
        mediaInfoIntent.putExtra(SONG_NAME, getProperty(doc, "npsong"));
        mediaInfoIntent.putExtra(FACT_TEXT, getProperty(doc, "npfact"));
        String imageUrl = getDjPicture(doc, "npdjimg");

        if (imageUrl != null)
        {
            mediaInfoIntent.putExtra(IMAGE_URL, imageUrl);
        }

        LocalBroadcastManager.getInstance(this).sendBroadcast(mediaInfoIntent);
        Log.d(LOG_TAG, "- leave handleMediaInfo: success");
    }

    private String getProperty(Document doc, String propName)
    {
        Elements elements = doc.getElementsByAttributeValue("id", propName);

        if (elements.size() > 0)
        {
            return elements.get(0).text();
        }

        return "";
    }

    private String getDjPicture(Document doc, String propName)
    {
        Elements elements = doc.getElementsByAttributeValue("id", propName);

        if (elements.size() > 0)
        {
            Element e = elements.get(0); // div
            elements = e.getElementsByAttribute("src");

            if (elements.size() > 0)
            {
                e = elements.get(0); // img
                Attributes attr = e.attributes();

                for (Attribute a: attr)
                {
                    if ("src".equals(a.getKey()))
                    {
                        String url = e.baseUri() + a.getValue();
                        Log.d(LOG_TAG, url);
                        return url;
                    }
                }
            }
        }

        return null;
    }

    // Handler that receives messages from the thread
    private final class MediaInfoServiceHandler extends Handler
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case START_COLLECTING_INFO:
                    timer = new Timer();
                    timerTask = createNewTimerTask();
                    timer.schedule(timerTask, 0, ONE_MINUTE);
                    break;
                case STOP_COLLECTING_INFO:
                    freeTimer();
                    break;
                case RESEND_INFO:
                    new HandleMediaInfoAsync().execute();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private TimerTask createNewTimerTask()
    {
        return new TimerTask() {
            Connection connection = Jsoup.connect(SI_RADIO_URL);

            public void run()
            {
                try
                {
                    handleMediaInfo(connection);
                }
                catch (Exception e)
                {
                    Log.e(LOG_TAG, e.getMessage(), e);
                }
            }
        };
    }

    private class HandleMediaInfoAsync extends AsyncTask<Void, Void, Void>
    {
        @Override
        protected Void doInBackground(Void... params)
        {
            try
            {
                handleMediaInfo(Jsoup.connect(SI_RADIO_URL));
            }
            catch (IOException e)
            {
                Log.e(LOG_TAG, e.getMessage(), e);
            }

            return null;
        }
    }
}
