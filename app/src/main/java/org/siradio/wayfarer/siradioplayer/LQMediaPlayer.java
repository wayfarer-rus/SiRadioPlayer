package org.siradio.wayfarer.siradioplayer;

import android.os.AsyncTask;
import android.util.Log;

import com.spoledge.aacplayer.AACPlayer;
import com.spoledge.aacplayer.ArrayAACPlayer;
import com.spoledge.aacplayer.ArrayDecoder;
import com.spoledge.aacplayer.Decoder;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class LQMediaPlayer implements SiMediaPlayer {
    private static final String URL = "http://listen.siradio.fm/mobile";
    private static final String ADDRESS = "listen.siradio.fm";
    private static final String GET_EVENT = "GET /mobile HTTP/1.0";

    private InputStream mStream;
    private AACPlayer lqMediaPlayer = null;
    private Socket mSocket;

    public void init(SiRadioPlayerService radioPlayerService) {
        if (lqMediaPlayer == null) {
            lqMediaPlayer = new ArrayAACPlayer(ArrayDecoder.create(Decoder.DECODER_FAAD2));
        }

        lqMediaPlayer.setPlayerCallback((LQRadioPlayerService)radioPlayerService);
        new RetrieveMediaStreamTaskAndStartPlayback().execute();
    }

    public void destroy() {
        if (lqMediaPlayer != null) {
            lqMediaPlayer.stop();
            lqMediaPlayer = null;
            closeConnection();
        }
    }

    public void start(SiRadioPlayerService lqRadioPlayerService) {
        if (lqMediaPlayer != null) {
            lqMediaPlayer.playAsync(mStream, 24);
        } else {
            init(lqRadioPlayerService);
        }
    }

    public boolean stop() {
        if (lqMediaPlayer != null) {
            lqMediaPlayer.stop();
            return true;
        }

        return false;
    }

    @Override
    public String getURL() {
        return URL;
    }

    private final class RetrieveMediaStreamTaskAndStartPlayback extends AsyncTask<Void, Void, InputStream> {
        private IOException exception;
        private Socket socket;

        @Override
        protected InputStream doInBackground(Void... params) {
            try {
                socket = new Socket(ADDRESS, 80);
                PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
                out.println(GET_EVENT);
                out.println();
                out.flush();
                return socket.getInputStream();
            } catch (IOException e) {
                this.exception = e;
                return null;
            }
        }

        protected void onPostExecute(InputStream result) {
            if (exception != null) {
                Log.e("StreamTask.PostExecute", exception.getMessage(), exception);
            } else {
                closeConnection();
                lqMediaPlayer.stop();
                mSocket = socket;
                mStream = result;
                lqMediaPlayer.playAsync(mStream, 24);
            }
        }

    }

    private void closeConnection() {
        try {
            if (mStream != null) {
                mStream.close();
                mSocket.close();
            }
        } catch (IOException e) {
            Log.e("LQMediaPlayer.destroy", e.getMessage(), e);
        }
    }
}
