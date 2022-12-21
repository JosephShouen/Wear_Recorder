package com.example.recorder_wear;

import android.Manifest;
import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import android.Manifest;
import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;

import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.app.PendingIntent.getActivity;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import static java.lang.Boolean.TRUE;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.recorder_wear.databinding.ActivityMainBinding;

public class MainActivity extends Activity {

    private ActivityMainBinding binding;
    private Button play_pause, record;
    private MediaPlayer mPlayer;

    private static String wav = null;
    private static String pcm = null;
    public static final int MULTIPLE_PERMISSIONS = 100;
    private static final int SAMPLING_RATE_IN_HZ = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    /**
     * Factor by that the minimum buffer size is multiplied. The bigger the factor is the less
     * likely it is that samples will be dropped, but more memory will be used. The minimum buffer
     * size is determined by {@link AudioRecord#getMinBufferSize(int, int, int)} and depends on the
     * recording settings.
     */
    private static final int BUFFER_SIZE_FACTOR = 2;

    /**
     * Size of the buffer where the audio data is stored by Android
     */
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLING_RATE_IN_HZ,
            CHANNEL_CONFIG, AUDIO_FORMAT) * BUFFER_SIZE_FACTOR;

    /**
     * Signals whether a recording is in progress (true) or not (false).
     */
    private static final AtomicBoolean recordingInProgress = new AtomicBoolean(false);
    private static AudioRecord recorder = null;
    private Thread recordingThread = null;

    //new
    final int bpp = 16;
    //new

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        checkPermission();

        play_pause = findViewById(R.id.play_pause);
        record = findViewById(R.id.record);

        String init_play_pauseText=play_pause.getText().toString();
        String init_recordText=record.getText().toString(); //изначальное значение кнопки record

        //если файл уже существует - значит он удалится при последующем запуске
        wav = getExternalCacheDir().getAbsolutePath();
        wav += "/recording.wav";
        pcm = getExternalCacheDir().getAbsolutePath();
        pcm += "/recording.pcm";

        record.setOnClickListener (new View.OnClickListener()
        {
            @Override
            public void onClick(View view) {
                String recordText = record.getText().toString();
                //если значение кнопки Start Record - стратуем, и меняем на Stop Record
                //иначе - стопуем
                if (recordText == init_recordText) {
                    if (startRecording()){
                        record.setText("Stop Record");
                    }
                } else {
                    pauseRecording();
                    record.setText(init_recordText);
                }
            }
        });

        play_pause.setOnClickListener (new View.OnClickListener()
        {
            @Override
            public void onClick(View view) {
                String play_pauseText=play_pause.getText().toString();
                //same shit
                if (play_pauseText == init_play_pauseText) {
                    playAudio();
                    play_pause.setText("Pause");
                } else {
                    pausePlaying();
                    play_pause.setText(init_play_pauseText);
                }

            }
        });
    }



    private boolean startRecording() {
        if ((ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) + ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) != PackageManager.PERMISSION_GRANTED) {
            checkPermission();
            return FALSE;
        } else {
            recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, SAMPLING_RATE_IN_HZ,
                    CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);
            recorder.startRecording();
            recordingInProgress.set(true);
            recordingThread = new Thread(new RecordingRunnable(), "Recording Thread");
            recordingThread.start();
            return TRUE;
        }
    }

    public void pauseRecording() {
        recordingInProgress.set(false);
        recorder.stop();
        recorder.release();
        recorder = null;
        recordingThread = null;
        createWavFile(pcm,wav);
    }

    //start
    static class RecordingRunnable implements Runnable {

        @Override
        public void run() {
            final File raw = new File(Environment.getExternalStorageDirectory(), "recording.pcm");
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
    //end

    //пока нерабочее
    public void playAudio() {

        // for playing our recorded audio
        // we are using media player class.
        mPlayer = new MediaPlayer();
        try {
        // below method is used to set the
        // data source which will be our file name
            final File wav_ss = new File(Environment.getExternalStorageDirectory(), "recording.wav");
            mPlayer.setDataSource(String.valueOf(wav_ss));

        // below method will prepare our media player
            mPlayer.prepare();

        // below method will start our media player.
            mPlayer.start();
        } catch (IOException e) {
           Log.e("TAG", "prepare() failed");
        }
    }

    public void pausePlaying() {
        // this method will release the media player
        // class and pause the playing of our recorded audio.
        mPlayer.release();
        mPlayer = null;
    }









    // function to check permissions
    private void checkPermission() {
        if ((ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) + ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale
                    (MainActivity.this, Manifest.permission.RECORD_AUDIO) ||
                    ActivityCompat.shouldShowRequestPermissionRationale
                            (MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(
                            new String[]{Manifest.permission
                                    .RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            MULTIPLE_PERMISSIONS);
                }

            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(
                            new String[]{Manifest.permission
                                    .RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            MULTIPLE_PERMISSIONS);
                }
            }
        } else {
//            Intent intent = new Intent(this, MainActivity2.class);
//            startActivity(intent );
//            MainActivity.this.finish();
//            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
//                    Uri.parse("package:" + getPackageName())));

//            Intent intent = new Intent(MainActivity.this, Test.class);
//            startActivity(intent );
//            MainActivity.this.finish();
        }
    }

    // Function to initiate after permissions are given by user
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MULTIPLE_PERMISSIONS:
                if (grantResults.length > 0) {
                    boolean recordPermission = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    boolean writeExternalFile = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if(recordPermission && writeExternalFile)
                    {
//                        Intent intent = new Intent(this, MainActivity2.class);
//                        startActivity(intent );
//                        MainActivity.this.finish();
//                        startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
//                                Uri.parse("package:" + getPackageName())));

//                        Intent intent = new Intent(MainActivity.this, Test.class);
//                        startActivity(intent );
//                        MainActivity.this.finish();
                    }
                }
                else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(
                                new String[]{Manifest.permission
                                        .WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO},
                                MULTIPLE_PERMISSIONS);
                    }
                }

        }
    }

    private void wavHeader(FileOutputStream fileOutputStream, long totalAudioLen, long totalDataLen, int channels, long byteRate){
        try {
            byte[] header = new byte[44];
            header[0] = 'R'; // RIFF/WAVE header
            header[1] = 'I';
            header[2] = 'F';
            header[3] = 'F';
            header[4] = (byte) (totalDataLen & 0xff);
            header[5] = (byte) ((totalDataLen >> 8) & 0xff);
            header[6] = (byte) ((totalDataLen >> 16) & 0xff);
            header[7] = (byte) ((totalDataLen >> 24) & 0xff);
            header[8] = 'W';
            header[9] = 'A';
            header[10] = 'V';
            header[11] = 'E';
            header[12] = 'f'; // 'fmt ' chunk
            header[13] = 'm';
            header[14] = 't';
            header[15] = ' ';
            header[16] = 16; // 4 bytes: size of 'fmt ' chunk
            header[17] = 0;
            header[18] = 0;
            header[19] = 0;
            header[20] = 1; // format = 1
            header[21] = 0;
            header[22] = (byte) channels;
            header[23] = 0;
            header[24] = (byte) ((long) SAMPLING_RATE_IN_HZ & 0xff);
            header[25] = (byte) (((long) SAMPLING_RATE_IN_HZ >> 8) & 0xff);
            header[26] = (byte) (((long) SAMPLING_RATE_IN_HZ >> 16) & 0xff);
            header[27] = (byte) (((long) SAMPLING_RATE_IN_HZ >> 24) & 0xff);
            header[28] = (byte) (byteRate & 0xff);
            header[29] = (byte) ((byteRate >> 8) & 0xff);
            header[30] = (byte) ((byteRate >> 16) & 0xff);
            header[31] = (byte) ((byteRate >> 24) & 0xff);
            header[32] = (byte) (2 * 16 / 8); // block align
            header[33] = 0;
            header[34] = bpp; // bits per sample
            header[35] = 0;
            header[36] = 'd';
            header[37] = 'a';
            header[38] = 't';
            header[39] = 'a';
            header[40] = (byte) (totalAudioLen & 0xff);
            header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
            header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
            header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
            fileOutputStream.write(header, 0, 44);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }


    public void createWavFile(String tempPath, String wavPath){
        try {
            final File raw_s = new File(Environment.getExternalStorageDirectory(), "recording.pcm");
            final File wav_s = new File(Environment.getExternalStorageDirectory(), "recording.wav");
            FileInputStream fileInputStream = new FileInputStream(raw_s);
            FileOutputStream fileOutputStream = new FileOutputStream(wav_s);
            byte[] data = new byte[BUFFER_SIZE];
            int channels = 1;
            long byteRate = bpp * SAMPLING_RATE_IN_HZ * channels / 8;
            long totalAudioLen = fileInputStream.getChannel().size();
            long totalDataLen = totalAudioLen + 36;
            wavHeader(fileOutputStream,totalAudioLen,totalDataLen,channels,byteRate);
            while (fileInputStream.read(data) != -1) {
                fileOutputStream.write(data);
            }
            fileInputStream.close();
            fileOutputStream.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

}