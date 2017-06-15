package wayfarer.siradioplayer

import android.os.AsyncTask
import android.util.Log

import com.spoledge.aacplayer.AACPlayer
import com.spoledge.aacplayer.ArrayAACPlayer
import com.spoledge.aacplayer.ArrayDecoder
import com.spoledge.aacplayer.Decoder

import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket

object LQMediaPlayer : SiMediaPlayer {

    private var mStream: InputStream? = null
    private var lqMediaPlayer: AACPlayer? = null
    private var mSocket: Socket? = null
    private var playerStarted = false

    override fun init() {
        if (lqMediaPlayer == null) {
            lqMediaPlayer = ArrayAACPlayer(ArrayDecoder.create(Decoder.DECODER_FAAD2))
        }

        RetrieveMediaStreamTaskAndStartPlayback().execute()
    }

    override fun destroy() {
        playerStarted = false
        lqMediaPlayer?.stop()
        lqMediaPlayer = null
        closeConnection()
    }

    override fun start() {
        if (playerStarted) return

        if (lqMediaPlayer != null) {
            lqMediaPlayer!!.playAsync(mStream, 24)
            playerStarted = true
        } else {
            init()
        }
    }

    override fun stop(): Boolean {
        playerStarted = false

        if (lqMediaPlayer != null) {
            lqMediaPlayer!!.stop()
            return true
        }

        return false
    }

    override val url: String
        get() = URL

    private class RetrieveMediaStreamTaskAndStartPlayback : AsyncTask<Void, Void, InputStream>() {
        private var exception: IOException? = null
        private var socket: Socket? = null

        override fun doInBackground(vararg params: Void): InputStream? {
            try {
                socket = Socket(ADDRESS, 80)
                val out = PrintWriter(BufferedWriter(OutputStreamWriter(socket!!.getOutputStream())))
                out.println(GET_EVENT)
                out.println()
                out.flush()
                return socket!!.getInputStream()
            } catch (e: IOException) {
                this.exception = e
                return null
            }

        }

        override fun onPostExecute(result: InputStream) {
            if (exception != null) {
                playerStarted = false
                Log.e("StreamTask.PostExecute", exception?.message, exception)
            } else {
                closeConnection()
                lqMediaPlayer?.stop()
                mSocket = socket
                mStream = result
                lqMediaPlayer?.playAsync(mStream, 24)
                playerStarted = true
            }
        }

    }

    private fun closeConnection() {
        try {
            mStream?.close()
            mSocket?.close()
        } catch (e: IOException) {
            Log.e("LQMediaPlayer.destroy", e.message, e)
        }
    }

    private val URL = "http://listen.siradio.fm/mobile"
    private val ADDRESS = "listen.siradio.fm"
    private val GET_EVENT = "GET /mobile HTTP/1.0"

}
