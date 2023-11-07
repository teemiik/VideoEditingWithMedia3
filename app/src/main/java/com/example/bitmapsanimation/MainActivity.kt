package com.example.bitmapsanimation

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.animation.LinearInterpolator
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.TextureOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.TransformationRequest
import androidx.media3.transformer.Transformer
import com.google.common.collect.ImmutableList
import java.io.File
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var bitmap1: Bitmap
    private var width: Int = 0
    private var height: Int = 0

    @SuppressLint("UnsafeOptInUsageError")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bitmap1 = BitmapFactory.decodeResource(this.resources, R.drawable.test1,
            BitmapFactory.Options().apply {
                inMutable = true
                inScaled = false
            })

        val path = Uri.parse("android.resource://" + packageName + "/" + R.raw.test_compressed)
        val pathResult = createNewFile(getMoviesExternalDir(), "test_result.mp4") ?: return

        val transformer = Transformer.Builder(this)
            .setTransformationRequest(
                TransformationRequest.Builder()
                    .setVideoMimeType(MimeTypes.VIDEO_H264)
                    .build()
            )
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    super.onCompleted(composition, exportResult)
                    val video = findViewById<VideoView>(R.id.video)
                    video.post {
                        video.setVideoPath(pathResult.absolutePath)
                        video.start()
                    }
                }

                override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                    super.onError(composition, exportResult, exportException)
                    exportException.printStackTrace()
                }
            })
            .build()


        val bitmapOverlay = object : BitmapOverlay() {
            override fun getBitmap(presentationTimeUs: Long): Bitmap {
                return drawFrame(presentationTimeUs)
            }
        }

        val overlaysBuilder = ImmutableList.Builder<TextureOverlay>()
        overlaysBuilder.add(bitmapOverlay)
        val overlays: ImmutableList<TextureOverlay> = overlaysBuilder.build()

        val inputMediaItem = MediaItem.fromUri(path)
        val editedMediaItem = EditedMediaItem.Builder(inputMediaItem)
            .setEffects(
                Effects(
                    listOf(),
                    listOf(OverlayEffect(overlays))
                )
            )
            .build()

        width = bitmap1.width
        height = bitmap1.height

        transformer.start(editedMediaItem, pathResult.toString())
    }

    private fun getMoviesExternalDir(): File? {
        val moviesDir = privateDirectory(Environment.DIRECTORY_MOVIES)
        return try {
            File(moviesDir, pathTest).also { if (!it.exists()) it.mkdirs() }
        } catch (e: Exception) {
            null
        }
    }

    private fun privateDirectory(directory: String, child: String = "Test"): File? {
        return try {
            File(this.getExternalFilesDir(directory), child).also { if (!it.exists()) it.mkdirs() }
        } catch (e: Exception) {
            null
        }
    }

    private fun createNewFile(dir: File?, fileName: String): File? {
        dir ?: return null
        return try {
            File(dir, fileName).apply { createNewFile() }
        } catch (e: Exception) {
            null
        }
    }

    val pathTest = "from_gallery_by_videos"

    private fun drawFrame(presentationTimeUs: Long): Bitmap {
        if (animatorSetTest == null) {
            initAnimation()
        }

        val correctTime = TimeUnit.MICROSECONDS.toMillis(presentationTimeUs)
        if (correctTime <= 2000) {
            animatorSetTest?.currentPlayTime = correctTime
        }

        val animation = animationData

        val mainBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(mainBitmap)

        val rotator = Matrix()
        rotator.postRotate(animation.rotation, width / 2f, height / 2f)

        canvas.drawBitmap(
            bitmap1,
            rotator,
            null
        )

        return mainBitmap
    }

    private var animationData: AnimationData = AnimationData(0f)
        set(value) {
            if (field != value) {
                field = value
            }
        }

    private var animatorSetTest: AnimatorSet? = null

    private fun initAnimation() {
        animatorSetTest = AnimatorSet().apply {
            val animation = ValueAnimator.ofFloat(0f, 360f).apply {
                duration = 2000L
                interpolator = LinearInterpolator()
                addUpdateListener {
                    val value = it.animatedValue as Float
                    animationData = animationData.copy(rotation = value)
                }
            }
            play(animation)
        }
    }

    data class AnimationData(
        val rotation: Float
    )
}