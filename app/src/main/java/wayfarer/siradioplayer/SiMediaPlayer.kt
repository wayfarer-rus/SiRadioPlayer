package wayfarer.siradioplayer

/**
 * Project SiRadioPlayer
 * Created by wayfarer on 4/4/15.
 */
interface SiMediaPlayer {
    fun init()
    fun destroy()
    fun start()
    fun stop(): Boolean
    val url: String
}
