package com.example.test.Music

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.support.annotation.IntegerRes
import android.view.View
import android.widget.*
import com.example.test.R
import java.io.File
import android.widget.TextView
import com.example.test.AppPreferences
import com.example.test.DataBase.Sound
import com.example.test.MainActivity
import kotlin.collections.ArrayList
import android.widget.AbsListView




class ClassMusic(val context: Context, val view: View?, val isWidget: Boolean) {
    var buttonPlayMusic: Button? = null
    var buttonPrevMusic: Button? = null
    var buttonNextMusic: Button? = null
    var listViewMusic: ListView? = null
    var seekBarMusic: SeekBar? = null
    var textView: TextView? = null

    init {
        if (firstCreate) {
            firstCreate = false
            db = DataBaseMusic(context)
            AsynkLoadSoundsFromBD().execute()
            AppPreferences.init(context)
        }
    }

    fun startSound() {
        val sound = db.readSound(AppPreferences.lastMusic)
        if (sound == null || !File(sound.path).isFile) {
            db.deleteSound(AppPreferences.lastMusic)
            AppPreferences.lastMusic = 0
            restoreVecSounds()
        } else {
            val uri = Uri.parse(sound.path)
            if (mediaPlayer.isPlaying)
                mediaPlayer.stop()
            listViewSelection = AppPreferences.lastMusic
            mediaPlayer = MediaPlayer.create(context, uri)
            mediaPlayer.start()
            sizeSound = mediaPlayer.duration
            seekBarMusic?.progress = 0
            seekBarMusic?.max = sizeSound
            setTextMusic(sound)
            startTimerSeekBar(mediaPlayer.duration.toLong())
            buttonPlayMusic?.text = "Pause"
            buttonPlayMusic?.setOnClickListener { _ ->
                onPauseMusic()
            }
        }
    }


    fun placeMusic() {
        if (!isWidget) {
            val sound = ClassMusic.db.readSound(AppPreferences.lastMusic)
            if (sound != null) {
                AppPreferences.lastMoment = mediaPlayer.currentPosition
                setTextMusic(sound)
                seekBarMusic?.max = sizeSound
                seekBarMusic?.progress = AppPreferences.lastMoment
                if (mediaPlayer.isPlaying) {
                    buttonPlayMusic?.setText("Pause")
                    buttonPlayMusic?.setOnClickListener { _ ->
                        onPauseMusic()
                    }
                } else {
                    buttonPlayMusic?.setOnClickListener { _ ->
                        startSound()
                    }
                }
            }
        }
    }

    private fun onPlayMusic() {
        startTimerSeekBar(ClassMusic.sizeSound - AppPreferences.lastMoment.toLong())
        buttonPlayMusic?.setText("Pause")
        mediaPlayer.seekTo(AppPreferences.lastMoment)
        mediaPlayer.start()
        buttonPlayMusic?.setOnClickListener { _ ->
            onPauseMusic()
        }
    }


    private fun onPauseMusic() {
        AppPreferences.lastMoment = ClassMusic.mediaPlayer.currentPosition
        ClassMusic.mediaPlayer.pause()
        ClassMusic.timerSeekbar?.cancel()
        buttonPlayMusic?.text ="Play"
        buttonPlayMusic?.setOnClickListener { _ ->
            onPlayMusic()
        }
    }

    private fun setTextMusic(sound: Sound) {
        textView?.text = (AppPreferences.lastMusic).toString() + ") " + sound.name + " (" + sound.duration + ")"
    }


    fun startTimerSeekBar(sizeSecSound: Long) {
        ClassMusic.timerSeekbar = object : CountDownTimer(sizeSecSound, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                seekBarMusic?.progress = ClassMusic.mediaPlayer.currentPosition
            }

            override fun onFinish() {
                if (ClassMusic.mediaPlayer.currentPosition >= ClassMusic.sizeSound - 1001) {
                    seekBarMusic?.progress = 0
                    AppPreferences.lastMusic++
                    startSound()
                }
            }
        }
        ClassMusic.timerSeekbar?.start()
    }

    fun changeSeekBar() {
        if (!isWidget) {
            if (sounds.size > 0 && !ClassMusic.firstCreate) {
                val moment = seekBarMusic?.progress
                if (moment != null) {
                    AppPreferences.lastMoment = moment
                    ClassMusic.mediaPlayer.seekTo(moment)
                }
                ClassMusic.timerSeekbar?.cancel()
                startTimerSeekBar((ClassMusic.sizeSound - AppPreferences.lastMoment).toLong())
            }
        }
    }


    fun setWidgets(play: Button, prev: Button, next: Button) {
        if (!isWidget) {
            seekBarMusic = view?.findViewById(R.id.SeekBarMusic)
            buttonPlayMusic = play
            buttonPrevMusic = prev
            buttonNextMusic = next
            textView = view?.findViewById(R.id.TextViewMusic)
            listViewMusic = view?.findViewById(R.id.ListViewMusic)
            seekBarMusic?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekbar: SeekBar?, progress: Int, fromUser: Boolean) {}
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekbar: SeekBar?) {
                    if (MainActivity.fragIndexSecond == 0) {
                        changeSeekBar()
                    }
                }
            })
            buttonPlayMusic?.setOnClickListener { _ ->
                if (MainActivity.fragIndexSecond == 0) {
                    startSound()
                }
            }
            buttonPrevMusic?.setOnClickListener { _ ->
                if (MainActivity.fragIndexSecond == 0) {
                    if (AppPreferences.lastMusic > 0)
                        AppPreferences.lastMusic--
                    else
                        AppPreferences.lastMusic = sounds.size - 1
                    if (!ClassMusic.mediaPlayer.isPlaying)
                        placeMusic()
                    else
                        startSound()
                    AppPreferences.lastMoment = 0
                    changeSeekBar()
                }
            }
            buttonNextMusic?.setOnClickListener { _ ->
                if (MainActivity.fragIndexSecond == 0) {
                    if (AppPreferences.lastMusic < sounds.size - 1)
                        AppPreferences.lastMusic++
                    else
                        AppPreferences.lastMusic = 0
                    if (!mediaPlayer.isPlaying)
                        placeMusic()
                    else
                        startSound()
                    AppPreferences.lastMoment = 0
                    changeSeekBar()
                }
            }
            if (sounds.size > listViewSelection)
                listViewMusic?.smoothScrollToPosition(listViewSelection)
        }
    }


    fun loadMusicListView() {
        val adapter = AdapterMusicFolder(context, sounds, this)
        listViewMusic?.adapter = adapter
        if (sounds.size > listViewSelection) {
            listViewMusic?.setSelection(ClassMusic.listViewSelection)
        }
    }


    fun restoreVecSounds() {
        if (!isWidget) {
            AsynkLoadSoundsFromBD().execute()
            loadMusicListView()
            val pos = listViewMusic?.selectedItemPosition
            if (pos != null && sounds.size > listViewSelection)
                ClassMusic.listViewSelection = pos
        }
    }

    fun searchContent(file: File) {
        if (file.isDirectory) {
            if (file.canRead()) {
                for (temp in file.listFiles()) {
                    if (!temp.isDirectory)
                        getContent(temp, file.name)
                }
            }
        } else {
            getContent(file, "*")
        }
    }

    fun getContent(temp: File, directory: String) {
        if (temp.absoluteFile.toString().contains(".mp3")) {
            val split = temp.absoluteFile.toString().split('/')
            val metaRetriever = MediaMetadataRetriever()
            metaRetriever.setDataSource(temp.absoluteFile.toString())
            val duration = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toInt() / 1000
            var strDuration = ""
            if (duration / 60.0 > 1) {
                strDuration += (duration / 60).toString() + ":"
                if ((duration % 60).toString().length > 1)
                    strDuration += (duration % 60).toString()
                else
                    strDuration += (duration % 60).toString() + "0"
            } else
                strDuration += duration.toString()

            db.insertSound(Sound(sounds.size, split[split.size - 1], temp.absoluteFile.toString(), strDuration, directory))
            countSound++
            textView?.text = "Load " + countSound.toString()
        }
    }


    companion object {
        var firstCreate = true

        var mediaPlayer: MediaPlayer = MediaPlayer()
        var timerSeekbar: CountDownTimer? = null
        lateinit var db: DataBaseMusic
        var listViewSelection = 0

        var sizeSound = 0
        var countSound = 0

        var sounds = ArrayList<Sound>()
    }

    inner class AsynkLoadSoundsToBD(val arr: ArrayList<String>) : AsyncTask<Void, IntegerRes, Void>() {
        override fun onPostExecute(result: Void?) {
            restoreVecSounds()
            textView?.setText("ClassMusic")
            super.onPostExecute(result)
        }

        override fun doInBackground(vararg params: Void?): Void? {
            for (value in arr)
                searchContent(File(value))
            return null
        }
    }

    inner class AsynkLoadSoundsFromBD : AsyncTask<Void, IntegerRes, Void>() {
        override fun onPostExecute(result: Void?) {
            if (!isWidget) {
                loadMusicListView()
            }
            super.onPostExecute(result)
        }

        override fun doInBackground(vararg params: Void?): Void? {
            sounds = db.readAllSound()
            return null
        }
    }

}