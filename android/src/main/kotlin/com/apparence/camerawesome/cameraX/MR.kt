import android.content.Context
import android.media.MediaRecorder
import android.os.Environment
import android.util.Log
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.core.content.ContextCompat
class CameraAwesomeX(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private var mediaRecorder: MediaRecorder? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var executor: ExecutorService = Executors.newSingleThreadExecutor()
    private var isRecording: Boolean = false
    private var videoFile: File? = null
    init {
        setupCameraProvider()
    }
    // Set up CameraX
    private fun setupCameraProvider() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            preview = Preview.Builder().build()
            // Set up the camera selector (default: back camera)
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            // Bind the preview to the lifecycle
            cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview
            )
        }, ContextCompat.getMainExecutor(context))
    }
    // Configure MediaRecorder for custom encoding
    private fun setupMediaRecorder(): MediaRecorder {
        val outputDirectory = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        videoFile = File(outputDirectory, "VIDEO_$timestamp.mp4")
        val recorder = MediaRecorder()
        recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        // Custom encoding parameters
        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        recorder.setVideoEncodingBitRate(3000000) // Bitrate
        recorder.setVideoFrameRate(30) // Frame rate
        recorder.setVideoSize(960, 720) // Resolution
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        recorder.setAudioEncodingBitRate(128000) // Audio bitrate
        recorder.setAudioSamplingRate(44100)
        recorder.setOutputFile(videoFile?.absolutePath)
        recorder.prepare()
        return recorder
    }
    // Start recording video
    fun startRecording() {
        if (isRecording) {
            Log.w("CameraAwesomeX", "Already recording")
            return
        }
        try {
            mediaRecorder = setupMediaRecorder()
            mediaRecorder?.start()
            isRecording = true
            Log.d("CameraAwesomeX", "Recording started")
        } catch (e: Exception) {
            Log.e("CameraAwesomeX", "Failed to start recording", e)
        }
    }
    // Stop recording video
    fun stopRecording(): File? {
        if (!isRecording) {
            Log.w("CameraAwesomeX", "Not recording")
            return null
        }
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            Log.d("CameraAwesomeX", "Recording stopped. File saved at: ${videoFile?.absolutePath}")
        } catch (e: Exception) {
            Log.e("CameraAwesomeX", "Failed to stop recording", e)
        }
        return videoFile
    }
    // Release camera resources
    fun release() {
        cameraProvider?.unbindAll()
        executor.shutdown()
    }
}