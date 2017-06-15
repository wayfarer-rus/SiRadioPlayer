package wayfarer.siradioplayer

import android.content.Context
import android.content.Intent
import android.os.Parcel
import android.os.Parcelable
import android.support.v4.content.LocalBroadcastManager
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.*
import kotlin.concurrent.fixedRateTimer

/**
 * Project SiRadioPlayer_Kotlin
 * Created by wayfarer on 05.06.17.
 */

class MediaInfo : Parcelable {
    var djOnAir: String? = null
    var titleName: String? = null
    var songName: String? = null
    var factText: String? = null
    var imageUrl: String? = null

    constructor()

    constructor(input: Parcel) {
        djOnAir = input.readString()
        titleName = input.readString()
        songName = input.readString()
        factText = input.readString()
        imageUrl = input.readString()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(out: Parcel?, flags: Int) {
        out!!.writeString(djOnAir)
        out.writeString(titleName)
        out.writeString(songName)
        out.writeString(factText)
        out.writeString(imageUrl)
    }

    val CREATOR: Parcelable.Creator<MediaInfo> = object : Parcelable.Creator<MediaInfo> {
        override fun createFromParcel(input: Parcel): MediaInfo {
            return MediaInfo(input)
        }

        override fun newArray(size: Int): Array<MediaInfo?> {
            return arrayOfNulls(size)
        }
    }

}

object MediaService {
    // one minute in milliseconds
    val ONE_MINUTE = (60 * 1000).toLong()
    // url to the site with media info
    private val SI_RADIO_URL = "http://siradio.fm/"

    private var timer: Timer? = null
    // application context from main activity
    var context: Context? = null

    fun collectMediaInfo(collect: Boolean) {
        if (collect) {
            timer = fixedRateTimer(name = "media-info-timer",
                    initialDelay = 0.toLong(),
                    period = ONE_MINUTE) {
                if (context != null)
                    downloadMediaInfo()
            }
        } else {
            timer?.cancel()
            timer = null
        }
    }

    private fun downloadMediaInfo() {
        val mediaInfo = MediaInfo()
        val connection = Jsoup.connect(SI_RADIO_URL)
        val doc = connection.get()

        mediaInfo.djOnAir   = getProperty(doc, "npdjonair")
        mediaInfo.titleName = getProperty(doc, "nptrack")
        mediaInfo.songName  = getProperty(doc, "npsong")
        mediaInfo.factText  = getProperty(doc, "npfact")
        mediaInfo.imageUrl  = getDjPicture(doc, "npdjimg")

        val intent = Intent(MainActivity.MediaInfoIntentOptions.MEDIA_INFO_MESSAGE)

        with(MainActivity.MediaInfoIntentOptions) {
            intent.message = mediaInfo
        }

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    private fun getProperty(doc: Document, propName: String): String {
        val elements = doc.getElementsByAttributeValue("id", propName)

        if (elements.size > 0) {
            return elements[0].text()
        }

        return ""
    }

    private fun getDjPicture(doc: Document, propName: String): String? {
        var elements = doc.getElementsByAttributeValue("id", propName)

        if (elements.size > 0) {
            var e = elements[0] // div
            elements = e.getElementsByAttribute("src")

            if (elements.size > 0) {
                e = elements[0] // img
                val attr = e.attributes()

                for ((key, value) in attr) {
                    if ("src" == key) {
                        val url = e.baseUri() + value
                        return url
                    }
                }
            }
        }

        return null
    }


}
