package org.siradio.wayfarer.siradioplayer;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
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

import java.util.Arrays;

public class SiRadioPlayer extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback
{
    private static final String LOG_TAG = "SiRadioPlayer";
    private static final String INFO_DJ = "djName";
    private static final String INFO_SONG = "songInfo";
    private static final String INFO_TITLE = "titleInfo";
    private static final String PLAYBACK_STATE = "playingState";

    private boolean mIsPlaying = false;
    private String mDjName;
    private String mTitle;
    private String mSong;

    private static final int MY_PERMISSIONS_REQUEST_INTERNET = 0;
    private static final int MY_PERMISSIONS_REQUEST_MEDIA_CONTENT_CONTROL = 1;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_NETWORK_STATE = 2;
    private static final int MY_PERMISSIONS_REQUEST_WAKE_LOCK = 3;

    private View mLayout;

    public void playRadio(View view)
    {
        checkMyPermissions(Manifest.permission.ACCESS_NETWORK_STATE, MY_PERMISSIONS_REQUEST_ACCESS_NETWORK_STATE);
        checkMyPermissions(Manifest.permission.INTERNET, MY_PERMISSIONS_REQUEST_INTERNET);

        view.setVisibility(View.GONE);
        View stopButton = findViewById(R.id.stopButton);
        stopButton.setVisibility(View.VISIBLE);
        mRadioServiceConnection.sendMessageToService(MasterRadioControllerService.PLAY);
        mIsPlaying = true;
    }

    public void stopRadioPlayback(View view)
    {
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

    public void updateMediaInfo(View view)
    {
        checkMyPermissions(Manifest.permission.INTERNET, MY_PERMISSIONS_REQUEST_INTERNET);
        mMediaInfoConnection.sendMessageToService(MediaInfoService.RESEND_INFO);
    }

    private void hideInfo()
    {
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

    private void doBindService()
    {
        // Bind to the service
        bindService(new Intent(this, MasterRadioControllerService.class), mRadioServiceConnection, Context.BIND_AUTO_CREATE);
        bindService(new Intent(this, MediaInfoService.class), mMediaInfoConnection, Context.BIND_AUTO_CREATE);
    }

    private void doUnbindService()
    {
        // Unbind from the service
        if (mRadioServiceConnection.isServiceBound())
        {
            mRadioServiceConnection.sendMessageToService(MasterRadioControllerService.DESTROY);
            unbindService(mRadioServiceConnection);
            mRadioServiceConnection.setServiceBound(false);
        }

        if (mMediaInfoConnection.isServiceBound())
        {
            unbindService(mMediaInfoConnection);
            mMediaInfoConnection.setServiceBound(false);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.d(LOG_TAG, "+ enter onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_si_radio_player);
        mLayout = findViewById(R.id.container);

        if (savedInstanceState == null)
        {
            Log.d(LOG_TAG, "create UI");
            getSupportFragmentManager().beginTransaction()
                .add(R.id.container, new PlaceholderFragment())
                .commit();
        }
        else
        {
            Log.d(LOG_TAG, "load instance state");
            mIsPlaying = savedInstanceState.getBoolean(PLAYBACK_STATE);

            if (mIsPlaying)
            {
                mDjName = String.valueOf(savedInstanceState.getCharSequence(INFO_DJ));
                mTitle = String.valueOf(savedInstanceState.getCharSequence(INFO_TITLE));
                mSong = String.valueOf(savedInstanceState.getCharSequence(INFO_SONG));
            }
        }

        Log.d(LOG_TAG, "init ImageLoader");
        ImageLoader.getInstance().init(ImageLoaderConfiguration.createDefault(this));
        Log.d(LOG_TAG, "register Receivers");
        // MediaInfo listener
        LocalBroadcastManager.getInstance(this).registerReceiver(mediaInfoReceiver, new IntentFilter(SiRadioPlayerService.MEDIA_INFO_ACTION));
        // PlayerStatus listener
        LocalBroadcastManager.getInstance(this).registerReceiver(playerStatusReceiver, new IntentFilter(MasterRadioControllerService.PLAYER_STATUS));
        // Audio noisy listener
        LocalBroadcastManager.getInstance(this).registerReceiver(audioBecomingNoisyReceiver, new IntentFilter(MusicIntentReceiver.AUDIO_BECOMES_NOISY));
        // Bind Playback services
        Log.d(LOG_TAG, "bind services");
        doBindService();
        Log.d(LOG_TAG, "RadioPlayerApp created");
        Log.d(LOG_TAG, "- leave onCereate");
    }

    private void checkMyPermissions(String permission, int permissionRequestTag)
    {
        Log.d(LOG_TAG, "+ enter checkMyPermissions(" + permission + ", " + permissionRequestTag + ")");
        int permissionCheck = ContextCompat.checkSelfPermission(this, permission);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED)
        {
            switch (permissionRequestTag)
            {
                case MY_PERMISSIONS_REQUEST_ACCESS_NETWORK_STATE:
                    requestNetworkAccessPermission();
                    Log.d(LOG_TAG, "- leave checkMyPermissions: Network Access permission requested");
                    return;
                case MY_PERMISSIONS_REQUEST_INTERNET:
                    requestInternetPermission();
                    Log.d(LOG_TAG, "- leave checkMypermissions: Internet permission requested");
                    return;
                default:
                    Log.d(LOG_TAG, "- leave checkMyPermissions: wrong RequestTag: " + permissionRequestTag);
                    return;
            }
        } 

        Log.d(LOG_TAG, "- leave checkMyPermissions: permission already granted");
    }

    private void requestInternetPermission() {
        Log.d(LOG_TAG, "+ enter requestInternetPermission()");

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.INTERNET))
        {
            Log.d(LOG_TAG, "Displaying internet permission rationale to provide additional context.");
            Snackbar.make(mLayout, "Internet permissions is required for displaying media information.",
                    Snackbar.LENGTH_INDEFINITE)
            .setAction(R.string.ok, new View.OnClickListener() {
                @Override
                public void onClick(View view)
                {
                    ActivityCompat.requestPermissions(SiRadioPlayer.this,
                            new String[]{Manifest.permission.INTERNET},
                            SiRadioPlayer.MY_PERMISSIONS_REQUEST_INTERNET);
                }
            }).show();
        }else
        {
            Log.d(LOG_TAG, "Requesting Internet permission directly!");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET},
                    SiRadioPlayer.MY_PERMISSIONS_REQUEST_INTERNET);
        }

        Log.d(LOG_TAG, "- leave requestInternetPermission");
    }

    private void requestNetworkAccessPermission()
    {
        Log.d(LOG_TAG, "+ enter requestNetworkAccessPermission()");

        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                                                                Manifest.permission.ACCESS_NETWORK_STATE))
        {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // For example if the user has previously denied the permission.
            Log.d(LOG_TAG,
                  "Displaying network permission rationale to provide additional context.");
            Snackbar.make(mLayout, R.string.permission_network_state_rationale,
                          Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View view)
                    {
                        ActivityCompat.requestPermissions(SiRadioPlayer.this,
                                                          new String[]{Manifest.permission.ACCESS_NETWORK_STATE},
                                                          SiRadioPlayer.MY_PERMISSIONS_REQUEST_ACCESS_NETWORK_STATE);
                    }
                })
                .show();
        }
        else
        {
            Log.d(LOG_TAG, "Requesting Network Access permission directly!");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_NETWORK_STATE},
                                              SiRadioPlayer.MY_PERMISSIONS_REQUEST_ACCESS_NETWORK_STATE);
        }

        Log.d(LOG_TAG, "- leave requestNetworkAccessPermission");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults)
    {
        // If request is cancelled, the result arrays are empty.
        switch (requestCode)
        {
            case MY_PERMISSIONS_REQUEST_INTERNET: {
                Log.d(LOG_TAG, "RequestPermissionsResult for INTERNET permission:");
                    break;
                }
            case MY_PERMISSIONS_REQUEST_WAKE_LOCK: {
                Log.d(LOG_TAG, "RequestPermissionsResult for WAKE_LOCK permission:");
                break;
                }
            case MY_PERMISSIONS_REQUEST_MEDIA_CONTENT_CONTROL: {
                Log.d(LOG_TAG, "RequestPermissionsResult for MEDIA_CONTENT_CONTROL permission:");
                break;
                }
            case MY_PERMISSIONS_REQUEST_ACCESS_NETWORK_STATE: {
                Log.d(LOG_TAG, "RequestPermissionsResult for ACCESS_NETWORK_STATE permission:");
                break;
                }
            default: {
                Log.d(LOG_TAG, requestCode + " is invalid permission code");
                    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                return;
                }
        }

        Log.d(LOG_TAG, "permissions list: " + Arrays.toString(permissions));
        Log.d(LOG_TAG, "grants list: " + Arrays.toString(grantResults));
        exitIfNotGranted(grantResults);
    }

    private void exitIfNotGranted(int[] grantResults)
    {
        if (grantResults.length > 0
            && grantResults[0] == PackageManager.PERMISSION_GRANTED)
        {
            Log.i(LOG_TAG, "Permission has now been granted. Showing preview.");
            Snackbar.make(mLayout, R.string.permision_available,
                          Snackbar.LENGTH_SHORT).show();
        }
        else
        {
            Log.i(LOG_TAG, "Permission was NOT granted.");
            Snackbar.make(mLayout, R.string.permissions_not_granted,
                          Snackbar.LENGTH_SHORT).show();

        }
    }

    private Notification.Action generateAction(int iconId, String title, String intentAction)
    {
        Intent intent = new Intent(this, MasterRadioControllerService.class);
        intent.setAction(intentAction);
        PendingIntent pendingIntent = PendingIntent.getService(this, 1, intent, 0);

        // TODO: change it with next update
        return new Notification.Action.Builder(iconId, title, pendingIntent).build();
    }

    private void buildNotification(Notification.Action action)
    {
        Notification.MediaStyle style = new Notification.MediaStyle();
        Intent resultIntent = new Intent(this, SiRadioPlayer.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        // get image bitmap from image view (it always contain actual picture) for notification dialog
        ImageView image = (ImageView)findViewById(R.id.djImage);
        Bitmap bitmap = ((BitmapDrawable)image.getDrawable()).getBitmap();
        // build notification
        Notification.Builder builder = new Notification.Builder(this)
            .setSmallIcon(R.mipmap.siradio_favicon_small)
            .setLargeIcon(bitmap)
            .setContentTitle(mTitle)
            .setContentText(mSong)
            .setContentIntent(pendingIntent)
            .setStyle(style)
            .setOngoing(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC);

        builder.addAction(action);
        style.setShowActionsInCompactView(0);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState != null)
        {
            mIsPlaying = savedInstanceState.getBoolean(PLAYBACK_STATE);

            if (mIsPlaying)
            {
                mDjName = String.valueOf(savedInstanceState.getCharSequence(INFO_DJ));
                mTitle = String.valueOf(savedInstanceState.getCharSequence(INFO_TITLE));
                mSong = String.valueOf(savedInstanceState.getCharSequence(INFO_SONG));
            }
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        updateButtonState();

        if (mIsPlaying) {
            updateNotification();
        }

        Log.d(LOG_TAG, "Resumed");
    }

    private void updateButtonState()
    {
        View stopButton = findViewById(R.id.stopButton);
        View startButton = findViewById(R.id.startButton);

        if (startButton != null && stopButton != null)
        {
            if (mIsPlaying)
            {
                startButton.setVisibility(View.GONE);
                stopButton.setVisibility(View.VISIBLE);
                updateScreen(mDjName, mTitle, mSong);
            }
            else
            {
                startButton.setVisibility(View.VISIBLE);
                stopButton.setVisibility(View.GONE);
                hideInfo();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        savedInstanceState.putCharSequence(INFO_DJ, mDjName);
        savedInstanceState.putCharSequence(INFO_SONG, mSong);
        savedInstanceState.putCharSequence(INFO_TITLE, mTitle);
        savedInstanceState.putBoolean(PLAYBACK_STATE, mIsPlaying);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onDestroy()
    {
        Log.d(LOG_TAG, "+ enter onDestroy()");
        Log.d(LOG_TAG, "get NotificationManager");
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Log.d(LOG_TAG, "close Notification");
        notificationManager.cancelAll();
        Log.d(LOG_TAG, "destroy ImageLoader");
        ImageLoader.getInstance().destroy();
        Log.d(LOG_TAG, "unbind services");
        doUnbindService();
        Log.d(LOG_TAG, "unregister Receivers");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mediaInfoReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(playerStatusReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(audioBecomingNoisyReceiver);
        Log.d(LOG_TAG, "RadioPlayerApp destroyed");
        super.onDestroy();
        Log.d(LOG_TAG, "- leave onDestroy()");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_si_radio_player, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private BroadcastReceiver mediaInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            Log.d(LOG_TAG, intent.getStringExtra(MediaInfoService.ON_AIR));
            Log.d(LOG_TAG, "Now Playing:");
            Log.d(LOG_TAG, intent.getStringExtra(MediaInfoService.TITLE_NAME));
            Log.d(LOG_TAG, "by");
            Log.d(LOG_TAG, intent.getStringExtra(MediaInfoService.SONG_NAME));
            Log.d(LOG_TAG, intent.getStringExtra(MediaInfoService.FACT_TEXT));

            // image
            if (intent.getStringExtra(MediaInfoService.IMAGE_URL) != null)
            {
                String imageUrl = intent.getStringExtra(MediaInfoService.IMAGE_URL);
                ImageView djImage = (ImageView)findViewById(R.id.djImage);
                ImageLoader.getInstance().displayImage(imageUrl, djImage);
                djImage.refreshDrawableState();
            }

            // Remember MediaInfo
            mDjName = intent.getStringExtra(MediaInfoService.ON_AIR);
            mTitle = intent.getStringExtra(MediaInfoService.TITLE_NAME);
            mSong = intent.getStringExtra(MediaInfoService.SONG_NAME);
            // Update screen
            updateScreen(mDjName, mTitle, mSong);
            // Update notification
            updateNotification();
        }
    };

    private void updateNotification() {
        Notification.Action action = null;

        if (mIsPlaying) {
            action = generateAction(R.drawable.stop_button_32, "Stop", MasterRadioControllerService.ACTION_STOP);
        } else {
            action = generateAction(R.drawable.play_button_32, "Play", MasterRadioControllerService.ACTION_PLAY);
        }

        buildNotification(action);
    }

    private void updateScreen(String djName, String titleName, String songName)
    {
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
        public void onReceive(Context context, Intent intent)
        {
            String status = intent.getStringExtra("status");

            if (status != null && !status.isEmpty())
            {
                Notification.Action action = null;
                switch (status)
                {
                    case "stopped":
                        mIsPlaying = false;
                        break;
                    case "started":
                        mIsPlaying = true;
                        break;
                }

                updateButtonState();
                updateNotification();
            }
        }
    };

    private BroadcastReceiver audioBecomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent)
        {
            mRadioServiceConnection.sendMessageToService(MasterRadioControllerService.STOP);
        }
    };

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment
    {

        public PlaceholderFragment()
        {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState)
        {
            return inflater.inflate(R.layout.fragment_si_radio_player, container, false);
        }
    }
}
