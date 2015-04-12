package org.siradio.wayfarer.siradioplayer;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;


public class SiRadioPlayer extends ActionBarActivity {
    private static final String LOG_TAG = "SiRadioPlayer";
    private static final String INFO_DJ = "djName";
    private static final String INFO_SONG = "songInfo";
    private static final String INFO_TITLE = "titleInfo";
    private static final String PLAYBACK_STATE = "playingState";

    private boolean mIsPlaying = false;
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;
    private String mDjName;
    private String mTitle;
    private String mSong;

    public void playRadio(View view) {
        view.setVisibility(View.GONE);
        View stopButton = findViewById(R.id.stopButton);
        stopButton.setVisibility(View.VISIBLE);
        mRadioServiceConnection.sendMessageToService(MasterRadioControllerService.PLAY);
        mIsPlaying = true;
    }

    public void stopRadioPlayback(View view) {
        // switch buttons
        view.setVisibility(View.GONE);
        View startButton = findViewById(R.id.startButton);
        startButton.setVisibility(View.VISIBLE);
        // Hide info
        hideInfo();
        // send message
        mRadioServiceConnection.sendMessageToService(MasterRadioControllerService.STOP);
        mIsPlaying = false;
    }

    private void hideInfo() {
        Log.d(LOG_TAG, "hide Info");
        ImageView djImage = (ImageView)findViewById(R.id.djImage);
        djImage.setImageResource(R.drawable.siradio_favicon_large);
        TextView onAir = (TextView)findViewById(R.id.onAir);
        TextView nowPlaying = (TextView)findViewById(R.id.nowPlaying);
        TextView title = (TextView)findViewById(R.id.track);
        TextView by = (TextView)findViewById(R.id.by);
        TextView song = (TextView)findViewById(R.id.song);
        onAir.setText(getString(R.string.greeting));
        nowPlaying.setVisibility(View.INVISIBLE);
        title.setVisibility(View.INVISIBLE);
        by.setVisibility(View.INVISIBLE);
        song.setVisibility(View.INVISIBLE);
    }

    private SiPlayerServiceConnection mRadioServiceConnection = new SiPlayerServiceConnection();
    private SiPlayerServiceConnection mMediaInfoConnection = new SiPlayerServiceConnection();

    private void doBindService() {
        // Bind to the service
        bindService(new Intent(this, MasterRadioControllerService.class), mRadioServiceConnection, Context.BIND_AUTO_CREATE);
        bindService(new Intent(this, MediaInfoService.class), mMediaInfoConnection, Context.BIND_AUTO_CREATE);
    }

    private void doUnbindService() {
        // Unbind from the service
        if (mRadioServiceConnection.isServiceBound()) {
            mRadioServiceConnection.sendMessageToService(MasterRadioControllerService.DESTROY);
            unbindService(mRadioServiceConnection);
            mRadioServiceConnection.setServiceBound(false);
        }

        if (mMediaInfoConnection.isServiceBound()) {
            unbindService(mMediaInfoConnection);
            mMediaInfoConnection.setServiceBound(false);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_si_radio_player);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        } else {
            mIsPlaying = savedInstanceState.getBoolean(PLAYBACK_STATE);

            if (mIsPlaying) {
                mDjName = String.valueOf(savedInstanceState.getCharSequence(INFO_DJ));
                mTitle = String.valueOf(savedInstanceState.getCharSequence(INFO_TITLE));
                mSong = String.valueOf(savedInstanceState.getCharSequence(INFO_SONG));
            }
        }

        ImageLoader.getInstance().init(ImageLoaderConfiguration.createDefault(this));
        // MediaInfo listener
        LocalBroadcastManager.getInstance(this).registerReceiver(mediaInfoReceiver, new IntentFilter(SiRadioPlayerService.MEDIA_INFO_ACTION));
        // PlayerStatus listener
        LocalBroadcastManager.getInstance(this).registerReceiver(playerStatusReceiver, new IntentFilter(MasterRadioControllerService.PLAYER_STATUS));
        // Bind Playback services
        doBindService();

        // fire notification info
        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(this, SiRadioPlayer.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack
        stackBuilder.addParentStack(SiRadioPlayer.class);
        // Adds the Intent to the top of the stack
        stackBuilder.addNextIntent(intent);
        // Gets a PendingIntent containing the entire back stack
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentTitle(getString(R.string.title))
                .setContentText(getString(R.string.greeting))
                .setSmallIcon(R.drawable.siradio_favicon_small)
                .setContentIntent(pendingIntent);
        Log.d(LOG_TAG, "RadioPlayerApp created");
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateButtonState();
        Log.d(LOG_TAG, "Resumed");
    }

    private void updateButtonState() {
        View stopButton = findViewById(R.id.stopButton);
        View startButton = findViewById(R.id.startButton);

        if (startButton != null && stopButton != null) {
            if (mIsPlaying) {
                startButton.setVisibility(View.GONE);
                stopButton.setVisibility(View.VISIBLE);
                updateScreen(mDjName, mTitle, mSong);
            } else {
                startButton.setVisibility(View.VISIBLE);
                stopButton.setVisibility(View.GONE);
                hideInfo();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putCharSequence(INFO_DJ, getTextFromView(R.id.onAir));
        savedInstanceState.putCharSequence(INFO_SONG, getTextFromView(R.id.song));
        savedInstanceState.putCharSequence(INFO_TITLE, getTextFromView(R.id.track));
        savedInstanceState.putBoolean(PLAYBACK_STATE, mIsPlaying);
        super.onSaveInstanceState(savedInstanceState);
    }

    private CharSequence getTextFromView(int viewId) {
        TextView view = (TextView)findViewById(viewId);

        if (view != null) {
            return view.getText();
        }

        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ImageLoader.getInstance().destroy();
        mNotifyManager.cancelAll();
        doUnbindService();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mediaInfoReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(playerStatusReceiver);
        Log.d(LOG_TAG, "RadioPlayerApp destroyed");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_si_radio_player, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private BroadcastReceiver mediaInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, intent.getStringExtra(MediaInfoService.ON_AIR));
            Log.d(LOG_TAG, "Now Playing:");
            Log.d(LOG_TAG, intent.getStringExtra(MediaInfoService.TITLE_NAME));
            Log.d(LOG_TAG, "by");
            Log.d(LOG_TAG, intent.getStringExtra(MediaInfoService.SONG_NAME));
            Log.d(LOG_TAG, intent.getStringExtra(MediaInfoService.FACT_TEXT));

            // image
            if (intent.getStringExtra(MediaInfoService.IMAGE_URL) != null) {
                String imageUrl = intent.getStringExtra(MediaInfoService.IMAGE_URL);
                ImageView djImage = (ImageView)findViewById(R.id.djImage);
                ImageLoader.getInstance().displayImage(imageUrl, djImage);
                djImage.refreshDrawableState();
            }

            mBuilder.setContentText(intent.getStringExtra(MediaInfoService.SONG_NAME) + " - " + intent.getStringExtra(MediaInfoService.TITLE_NAME));
            mNotifyManager.notify(0, mBuilder.build());
            // Update screen
            updateScreen(intent.getStringExtra(MediaInfoService.ON_AIR), intent.getStringExtra(MediaInfoService.TITLE_NAME), intent.getStringExtra(MediaInfoService.SONG_NAME));
        }
    };

    private void updateScreen(String djName, String titleName, String songName) {
        TextView onAir = (TextView)findViewById(R.id.onAir);
        TextView nowPlaying = (TextView)findViewById(R.id.nowPlaying);
        TextView title = (TextView)findViewById(R.id.track);
        TextView by = (TextView)findViewById(R.id.by);
        TextView song = (TextView)findViewById(R.id.song);
        onAir.setText(djName);
        nowPlaying.setVisibility(View.VISIBLE);
        title.setText(titleName);
        title.setVisibility(View.VISIBLE);
        by.setVisibility(View.VISIBLE);
        song.setText(songName);
        song.setVisibility(View.VISIBLE);
    }

    private BroadcastReceiver playerStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String status = intent.getStringExtra("status");

            if (status != null && !status.isEmpty()) {
                switch (status) {
                    case "stopped":
                        mIsPlaying = false;
                        break;
                    case "started":
                        mIsPlaying = true;
                        break;
                }

                updateButtonState();
            }
        }
    };

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_si_radio_player, container, false);
        }
    }
}
