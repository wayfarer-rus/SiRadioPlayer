package org.siradio.wayfarer.siradioplayer;

import android.media.MediaPlayer;

import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest  {

    private File mediaFile;

    @Test
    public void playAudio() {
        final String mediaUrl = "listen.siradio.fm";

        try {
            Socket socket = new Socket(mediaUrl,80);
            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
            out.println("GET /mobile HTTP/1.0");
            out.println();
            out.flush();
            InputStream is = socket.getInputStream();

            // create file to store audio
            mediaFile = new File("./mediaFile");
            FileOutputStream fos = new FileOutputStream(mediaFile);
            byte buf[] = new byte[24*1024];
            final long size = 30 * 1024 * 24; // 24 kbs * 30 sec
            System.out.println("FileOutputStream Download");

            // write to file until complete
            long readedBytes = 0;
            do {
                int numread = is.read(buf);

                if (numread <= 0)
                    break;

                fos.write(buf, 0, numread);

                readedBytes += numread;
                System.out.println("read " + numread + " bytes. " + (size - readedBytes) + " left");
            } while (readedBytes < size);
            fos.flush();
            fos.close();
            System.out.println("FileOutputStream Saved");
            MediaPlayer mp = new MediaPlayer();

            // create listener to tidy up after playback complete
            MediaPlayer.OnCompletionListener listener = new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    // free up media player
                    mp.release();
                    System.out.println("MP.OnCompletionListener MediaPlayer Released");
                }
            };
            mp.setOnCompletionListener(listener);

            FileInputStream fis = new FileInputStream(mediaFile);
            // set mediaplayer data source to file descriptor of input stream
            mp.setDataSource(fis.getFD());
            mp.prepare();
            System.out.println("MediaPlayer Start Player");
            mp.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}