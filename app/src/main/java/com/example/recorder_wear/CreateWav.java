package com.example.recorder_wear;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class CreateWav {

    int SAMPLING_RATE_IN_HZ;
    byte bpp;
    int BUFFER_SIZE;

    public CreateWav(int SAMPLING_RATE_IN_HZ, byte bpp, int BUFFER_SIZE)
    {
        this.SAMPLING_RATE_IN_HZ = SAMPLING_RATE_IN_HZ;
        this.bpp = bpp;
        this.BUFFER_SIZE = BUFFER_SIZE;
    }

    private void wavHeader(FileOutputStream fileOutputStream, long totalAudioLen, long totalDataLen, int channels, long byteRate){
        try {
            byte[] header = new byte[44];
            header[0] = 'R';
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
            header[12] = 'f';
            header[13] = 'm';
            header[14] = 't';
            header[15] = ' ';
            header[16] = 16;
            header[17] = 0;
            header[18] = 0;
            header[19] = 0;
            header[20] = 1;
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
            header[32] = (byte) (2 * 16 / 8);
            header[33] = 0;
            header[34] = bpp;
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

    public void createWavFile(File raw, File wav){
        try {
            FileInputStream fileInputStream = new FileInputStream(raw);
            FileOutputStream fileOutputStream = new FileOutputStream(wav);
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
            raw.delete();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
