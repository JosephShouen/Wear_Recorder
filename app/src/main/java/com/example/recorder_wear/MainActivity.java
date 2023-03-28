package com.example.recorder_wear;

import android.Manifest;
import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Bundle;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Environment;
import android.widget.ImageButton;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import com.example.recorder_wear.databinding.ActivityMainBinding;

public class MainActivity extends Activity {

    private ActivityMainBinding binding;
    private ImageButton imageRecord;
    static File raw;
    static File wav;
    public static final int MULTIPLE_PERMISSIONS = 100;
    private static final int SAMPLING_RATE_IN_HZ = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE_FACTOR = 2;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLING_RATE_IN_HZ,
            CHANNEL_CONFIG, AUDIO_FORMAT) * BUFFER_SIZE_FACTOR;
    private static final AtomicBoolean recordingInProgress = new AtomicBoolean(false);
    private static AudioRecord recorder = null;
    private Thread recordingThread = null;
    final int bpp = 16;
    int checkRecord;

    CreateWav create_file = new CreateWav(SAMPLING_RATE_IN_HZ, (byte) bpp, BUFFER_SIZE);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        checkPermission();
        imageRecord = findViewById(R.id.imageRecord);
        checkRecord = 0;

        imageRecord.setOnClickListener (view -> {
            if (checkRecord == 0)
            {
                startRecording();
                imageRecord.setImageResource(R.drawable.pause);
                checkRecord = 1;
            } else
            {
                pauseRecording();
                imageRecord.setImageResource(R.drawable.microphone);
                checkRecord = 0;
            }
        });
    }



    private void startRecording() {
        if ((ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) + ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) != PackageManager.PERMISSION_GRANTED) {
            checkPermission();
        } else {
            recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, SAMPLING_RATE_IN_HZ,
                    CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);
            recorder.startRecording();
            recordingInProgress.set(true);
            recordingThread = new Thread(new RecordingRunnable(), "Recording Thread");
            recordingThread.start();
        }
    }

    public void pauseRecording() {
        recordingInProgress.set(false);
        recorder.stop();
        recorder.release();
        recorder = null;
        recordingThread = null;
        create_file.createWavFile(raw, wav);
    }

    static class RecordingRunnable implements Runnable {

        @Override
        public void run() {
            Date date = Calendar.getInstance().getTime();
            DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd_hh:mm:ss");
            String strDate = dateFormat.format(date);
            File test = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/WearableRecords");
            test.mkdir();
            raw = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/WearableRecords", strDate+".pcm");
            wav = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/WearableRecords", strDate+".wav");

            final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

            try (final FileOutputStream outStream = new FileOutputStream(raw)) {
                while (recordingInProgress.get()) {
                    int result = recorder.read(buffer, BUFFER_SIZE);
                    if (result < 0) {
                        throw new RuntimeException("Reading of audio buffer failed: " +
                                getBufferReadFailureReason(result));
                    }
                    outStream.write(buffer.array(), 0, BUFFER_SIZE);
                    buffer.clear();
                }
            } catch (IOException e) {
                throw new RuntimeException("Writing of recorded audio failed", e);
            }
        }

        private String getBufferReadFailureReason(int errorCode) {
            switch (errorCode) {
                case AudioRecord.ERROR_INVALID_OPERATION:
                    return "ERROR_INVALID_OPERATION";
                case AudioRecord.ERROR_BAD_VALUE:
                    return "ERROR_BAD_VALUE";
                case AudioRecord.ERROR_DEAD_OBJECT:
                    return "ERROR_DEAD_OBJECT";
                case AudioRecord.ERROR:
                    return "ERROR";
                default:
                    return "Unknown (" + errorCode + ")";
            }
        }
    }


    private void checkPermission() {
        if ((ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) + ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission
                            .RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MULTIPLE_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if ((requestCode == MULTIPLE_PERMISSIONS) & (grantResults.length > 0)) {
                requestPermissions(
                        new String[]{Manifest.permission
                                .WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO},
                        MULTIPLE_PERMISSIONS);
        }
    }
}