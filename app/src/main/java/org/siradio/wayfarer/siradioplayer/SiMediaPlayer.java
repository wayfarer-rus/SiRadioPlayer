package org.siradio.wayfarer.siradioplayer;

/**
 * Project SiRadioPlayer
 * Created by wayfarer on 4/4/15.
 */
public interface SiMediaPlayer {
    public void init(SiRadioPlayerService radioPlayerService);
    public void destroy();
    public void start(SiRadioPlayerService radioPlayerService);
    public boolean stop();
    public String getURL();
}
