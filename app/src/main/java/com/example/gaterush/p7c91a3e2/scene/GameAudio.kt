package com.example.gaterush.p7c91a3e2.scene

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.sqrt

/** Soft mono cues, restrained stereo placement, and no resumption of stale one-shots. */
internal class GameAudio(context:Context):AutoCloseable {
    private data class Cue(val gain:Float,val gapMs:Long,val priority:Int,val durationMs:Long)
    private data class Stream(val expiresAt:Long,val peak:Float)
    private val cues=mapOf(
        "shot" to Cue(.15f,90,1,120), "impact" to Cue(.11f,150,1,140),
        "gate" to Cue(.38f,130,4,340), "enemy" to Cue(.22f,150,3,230),
        "blast" to Cue(.36f,180,4,440), "pickup" to Cue(.38f,220,5,390),
        "shield" to Cue(.36f,180,5,300), "hurt" to Cue(.33f,200,5,250),
        "clear" to Cue(.40f,800,6,640))
    private val pool=SoundPool.Builder().setMaxStreams(8).setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
    private val sounds=ConcurrentHashMap<String,Int>()
    private val loaded=ConcurrentHashMap.newKeySet<Int>()
    private val lastPlayed=mutableMapOf<String,Long>()
    private val counts=mutableMapOf<String,Int>()
    private val streams=mutableMapOf<Int,Stream>()
    private var active=false
    private var duckUntil=0L
    @Volatile private var closed=false

    init {
        pool.setOnLoadCompleteListener { _,id,status ->
            if(!closed) {
                if(status==0) {
                    loaded.add(id)
                    Log.i("GateRush","AUDIO_LOADED id=$id ready=${loaded.size}/${cues.size}")
                } else Log.e("GateRush","AUDIO_LOAD_FAILED id=$id status=$status")
            }
        }
        cues.keys.forEach { name ->
            try { context.assets.openFd("audio/$name.wav").use { sounds[name]=pool.load(it,1) } }
            catch(error:Exception) { Log.e("GateRush","AUDIO_ASSET_FAILED cue=$name",error) }
        }
    }

    @Synchronized fun setActive(value:Boolean) {
        if(closed || active==value) return
        active=value
        if(!value) {
            streams.keys.forEach { pool.stop(it) }
            streams.clear(); lastPlayed.clear(); duckUntil=0L
        }
        Log.i("GateRush","AUDIO_ACTIVE value=$value")
    }

    @Synchronized fun play(name:String,x:Float=0f) {
        if(closed || !active) return
        val cue=cues[name] ?: return
        val id=sounds[name] ?: return
        if(id !in loaded) return
        val now=SystemClock.uptimeMillis()
        if(now-(lastPlayed[name] ?: -10000L)<cue.gapMs) return
        streams.entries.removeAll { it.value.expiresAt<=now }
        // Collapse dense pellets and preserve room for more meaningful reward/hurt cues.
        if(cue.priority==1 && streams.size>=4) return
        val balance=(x/.36f).coerceIn(-.5f,.5f)
        val channelFactor=max(sqrt(1f-balance),sqrt(1f+balance))
        val desired=cue.gain*if(cue.priority==1 && now<duckUntil) .60f else 1f
        // Every source peak is <= .65; reserve headroom even for coincident maxima.
        val headroom=.80f-streams.values.sumOf { it.peak.toDouble() }.toFloat()
        val gain=minOf(desired,(headroom/(.65f*channelFactor)).coerceAtLeast(0f))
        if(gain<.05f) return
        val stream=pool.play(id,gain*sqrt(1f-balance),gain*sqrt(1f+balance),cue.priority,0,1f)
        if(stream==0) {
            Log.w("GateRush","AUDIO_PLAY_FAILED cue=$name")
            return
        }
        streams[stream]=Stream(now+cue.durationMs,.65f*gain*channelFactor)
        lastPlayed[name]=now
        if(cue.priority>=3) duckUntil=now+180
        val count=(counts[name] ?: 0)+1; counts[name]=count
        if(count<=3 || count%64==0)
            Log.i("GateRush","AUDIO_PLAY cue=$name stream=$stream count=$count gain=$gain")
    }

    @Synchronized override fun close() {
        if(closed) return
        setActive(false); closed=true
        pool.setOnLoadCompleteListener(null); pool.release()
        loaded.clear(); sounds.clear(); streams.clear()
        Log.i("GateRush","AUDIO_DISPOSED plays=$counts")
    }
}
