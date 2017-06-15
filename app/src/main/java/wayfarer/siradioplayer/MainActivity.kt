package wayfarer.siradioplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.nostra13.universalimageloader.core.ImageLoader
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() , AudioManager.OnAudioFocusChangeListener{
    private val LOG = "MainActivity"
    private val INFO_DJ = "djNameInfo"
    private val INFO_SONG = "songInfo"
    private val INFO_TITLE = "titleInfo"
    private val INFO_IMAGE_URL = "imageUrlInfo"
    private val PLAYBACK_STATE = "playingState"

    private val player : SiMediaPlayer? = LQMediaPlayer
    private val media : MediaService? = MediaService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        restoreState(savedInstanceState)
        media!!.context = this
        // init ImageLoader
        ImageLoader.getInstance().init(ImageLoaderConfiguration.createDefault(this))
        // MediaInfo listener
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mediaInfoReceiver, IntentFilter(MainActivity.MediaInfoIntentOptions.MEDIA_INFO_MESSAGE))
        // Setup an AudioFocusListener
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)

        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.e(LOG, "could not get audio focus.")
        }

        // Registers BroadcastReceiver to track network connection changes.
        this.registerReceiver(networkReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))

        val connMgr = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeInfo = connMgr.activeNetworkInfo
        // TODO:: get current network state
    }

    fun playButtonPressed(play_button: View) {
        Log.d(LOG, "player will be started soon")
        // swap buttons
        stop_button.visibility = View.VISIBLE
        play_button.visibility = View.INVISIBLE
        // start playback
        player!!.start()
        mIsPlaying = true
        media!!.collectMediaInfo(true)
    }

    fun stopButtonPressed(stop_button: View) {
        // swap buttons
        play_button.visibility = View.VISIBLE
        stop_button.visibility = View.INVISIBLE
        // stop playback
        player!!.destroy()
        media!!.collectMediaInfo(false)
        mIsPlaying = false
        hideInfo()
    }

    private fun hideInfo() {
        Log.d(LOG, "hideInfo is called")
        // hide song info
        nowPlaying.visibility = View.INVISIBLE
        track.visibility = View.INVISIBLE
        by.visibility = View.INVISIBLE
        song.visibility = View.INVISIBLE
        // reset greetings txt
        onAir.setText(R.string.greeting)
        // reset image to default
        logoImageView.setImageResource(R.drawable.siradio_favicon_large)
    }

    private fun showInfo() {
        Log.d(LOG, "showInfo is called")

        if (!mMediaInfo?.djOnAir.isNullOrBlank()) {
            nowPlaying.visibility = View.VISIBLE
            nowPlaying.text = mMediaInfo!!.djOnAir
        }

        if (!mMediaInfo?.titleName.isNullOrBlank()) {
            track.visibility = View.VISIBLE
            track.text = mMediaInfo!!.titleName
        }

        if (!mMediaInfo?.songName.isNullOrBlank()) {
            by.visibility = View.VISIBLE
            song.visibility = View.VISIBLE
            song.text = mMediaInfo!!.songName
        }

        if (!mMediaInfo?.imageUrl.isNullOrBlank()) {
            ImageLoader.getInstance().displayImage(mMediaInfo!!.imageUrl, logoImageView)
            logoImageView.refreshDrawableState()
        }

        /*if (!mMediaInfo?.factText.isNullOrBlank()) {
            onAir.text = mMediaInfo!!.factText
        }*/
    }

    override fun onDestroy() {
        player!!.destroy()
        this.unregisterReceiver(networkReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mediaInfoReceiver)
        ImageLoader.getInstance().destroy()
        super.onDestroy()
    }

    private var mMediaInfo: MediaInfo? = null
    private var mIsPlaying: Boolean = false

    private fun getDataFromIntent(mIntent: Intent) {
        Log.d(LOG, "get data from media-info-intent")
        var m: MediaInfo? = null

        with(MainActivity.MediaInfoIntentOptions) {
            m = mIntent.message
        }

        mMediaInfo = m
        Log.d(LOG, "${m?.djOnAir}; ${m?.songName}; ${m?.titleName}; ${m?.imageUrl}")

        if (mIsPlaying)
        {
            showInfo()
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putString(INFO_DJ, mMediaInfo?.djOnAir)
        outState?.putString(INFO_SONG, mMediaInfo?.songName)
        outState?.putString(INFO_TITLE, mMediaInfo?.titleName)
        outState?.putString(INFO_IMAGE_URL, mMediaInfo?.imageUrl)
        outState?.putBoolean(PLAYBACK_STATE, mIsPlaying)

        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        restoreState(savedInstanceState)
    }

    private fun restoreState(savedInstanceState: Bundle?) {
        if (mMediaInfo == null) mMediaInfo = MediaInfo()

        mMediaInfo!!.djOnAir = savedInstanceState?.getString(INFO_DJ)
        mMediaInfo!!.songName = savedInstanceState?.getString(INFO_SONG)
        mMediaInfo!!.titleName = savedInstanceState?.getString(INFO_TITLE)
        mMediaInfo!!.imageUrl = savedInstanceState?.getString(INFO_IMAGE_URL)

        if (savedInstanceState != null) {
            mIsPlaying = savedInstanceState.getBoolean(PLAYBACK_STATE, false)
        } else {
            mIsPlaying = false
        }

        if (mIsPlaying) {
            showInfo()
        } else {
            hideInfo()
        }

        resetButtonState(mIsPlaying)
    }

    private fun resetButtonState(isPlaying: Boolean) {
        if (isPlaying) {
            stop_button.visibility = View.VISIBLE
            play_button.visibility = View.INVISIBLE
        } else {
            play_button.visibility = View.VISIBLE
            stop_button.visibility = View.INVISIBLE
        }
    }

    /*
        Audio focus handling
     */
    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                // resume playback
                // TODO::
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                // Lost focus for an unbounded amount of time: stop playback and release media player
                // TODO::
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                // TODO::
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                // TODO::
            }
        }
    }

    /*
        Media information handling
     */
    object MediaInfoIntentOptions {
        const val MEDIA_INFO_MESSAGE = "wayfarer.siradioplayer.MainActivity::media-info"

        var Intent.message: MediaInfo?
            get() = getParcelableExtra<MediaInfo>(MEDIA_INFO_MESSAGE)
            set(message) {
                putExtra(MEDIA_INFO_MESSAGE, message)
            }
    }

    private val mediaInfoReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, mIntent: Intent) {
            getDataFromIntent(mIntent)
        }
    }

    /*
        Network status handling
     */
    // The BroadcastReceiver that tracks network connectivity changes.
    private val networkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, i: Intent) {
            val conn = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = conn.activeNetworkInfo
            var isConnectionActive : Boolean

            if (networkInfo != null && networkInfo.type == ConnectivityManager.TYPE_WIFI) {
                Log.d(LOG, "WiFi Connection Detected")
                isConnectionActive = true
            } else if (networkInfo != null && networkInfo.type == ConnectivityManager.TYPE_MOBILE) {
                Log.d(LOG, "Mobile Connection Detected")
                isConnectionActive = true
            } else {
                Log.d(LOG, "Connection Lost")
                isConnectionActive = false
            }

            // TODO:: Consider previous connection type
            // otherwise player will crash, due to switching from WIFI to Mobile is not so transparent \
            // for network streams
            if (!isConnectionActive && mIsPlaying) {
                player!!.destroy()
            } else if (isConnectionActive && mIsPlaying) {
                player!!.start()
            }
        }

    }

}
