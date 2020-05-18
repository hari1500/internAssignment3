package com.example.assignment3;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.ResultReceiver;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class DownloadService extends JobIntentService {
    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        File outputFile;
        ResultReceiver resultReceiver = intent.getParcelableExtra(Utils.IntentAndBundleKeys.resultReceiverKey);
        Bundle bundle = new Bundle();
        bundle.putInt(Utils.IntentAndBundleKeys.downloadStatusKey, Utils.DownloadStatuses.PENDING);
        bundle.putInt(Utils.IntentAndBundleKeys.progressPercentKey, 0);
        assert resultReceiver != null;

        String sourceUrl = intent.getStringExtra(Utils.IntentAndBundleKeys.sourceUrlKey);
        if (sourceUrl == null) {
            bundle.putInt(Utils.IntentAndBundleKeys.downloadStatusKey, Utils.DownloadStatuses.IMPROPER_URL);
            resultReceiver.send(Activity.RESULT_OK, bundle);
            return;
        }

        try {
            URL url = new URL(sourceUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                bundle.putInt(Utils.IntentAndBundleKeys.downloadStatusKey, Utils.DownloadStatuses.CONNECTION_FAILED);
                resultReceiver.send(Activity.RESULT_OK, bundle);
                return;
            }

            // Checks for existence of SD card
            File outputDirectory;
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                outputDirectory = new File(
                        Environment.getExternalStorageDirectory().getAbsolutePath(),
                        Utils.downloadDirectoryPath
                );
            } else {
                bundle.putInt(Utils.IntentAndBundleKeys.downloadStatusKey, Utils.DownloadStatuses.SD_CARD_NOT_EXISTS);
                resultReceiver.send(Activity.RESULT_OK, bundle);
                return;
            }

            if (!outputDirectory.exists()) {
                boolean directoryCreated = outputDirectory.mkdir();
                if (!directoryCreated) {
                    bundle.putInt(Utils.IntentAndBundleKeys.downloadStatusKey, Utils.DownloadStatuses.OUTPUT_DIR_CREATION_FAILED);
                    resultReceiver.send(Activity.RESULT_OK, bundle);
                    return;
                }
            }

            String outputFileName = Utils.sourceUrl.substring(Utils.sourceUrl.lastIndexOf('/')+1);
            outputFile = new File(outputDirectory, outputFileName);
            if (!outputFile.exists()) {
                boolean fileCreated = outputFile.createNewFile();
                if (!fileCreated) {
                    bundle.putInt(Utils.IntentAndBundleKeys.downloadStatusKey, Utils.DownloadStatuses.OUTPUT_FILE_CREATION_FAILED);
                    resultReceiver.send(Activity.RESULT_OK, bundle);
                    return;
                }
            }

            bundle.putInt(Utils.IntentAndBundleKeys.downloadStatusKey, Utils.DownloadStatuses.ONGOING);
            resultReceiver.send(Activity.RESULT_OK, bundle);

            FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
            InputStream inputStream = connection.getInputStream();
            byte[] buffer = new byte[1024];
            int nBytesRead, nBytesCumm = 0, fileSize = connection.getContentLength();
            while ((nBytesRead = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, nBytesRead);
                nBytesCumm += nBytesRead;

                int progressPercent = ((nBytesCumm * 100) / fileSize);
                bundle.putInt(Utils.IntentAndBundleKeys.progressPercentKey, progressPercent);
                resultReceiver.send(Activity.RESULT_OK, bundle);
                Log.v(Utils.logTag, ""+progressPercent);
            }

            fileOutputStream.close();
            inputStream.close();
            connection.disconnect();

            bundle.putInt(Utils.IntentAndBundleKeys.downloadStatusKey, Utils.DownloadStatuses.COMPLETED);
            resultReceiver.send(Activity.RESULT_OK, bundle);
            Log.v(Utils.logTag, "Download Completed......");
        } catch (IOException e) {
            e.printStackTrace();
            Log.v(Utils.logTag, e.toString());

            bundle.putInt(Utils.IntentAndBundleKeys.downloadStatusKey, Utils.DownloadStatuses.FAILED);
            resultReceiver.send(Activity.RESULT_OK, bundle);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(Utils.logTag, "Destroying DownloadService");
    }
}
