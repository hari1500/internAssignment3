package com.example.assignment3;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadServiceForeground extends Service {
    private NotificationManager manager;
    private static final String channelID = "DownloadServiceForeground";
    private static final String serviceChannelName = "Download Service Channel Foreground";
    private static final String notificationTitle = "Assignment 3";
    private static final String notificationTickerText = "Downloading...";
    private static final int ongoingNotificationId = 1;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        new DownloadTask(intent, startId).execute();

        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.v(Utils.logTag, "Destroying foreground service");
    }

    private void startInForeground(String fileName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    channelID,
                    serviceChannelName,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, channelID)
                .setContentTitle(notificationTitle)
                .setContentText(String.format("Downloading file %s", fileName))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setTicker(notificationTickerText)
                .build();
        startForeground(ongoingNotificationId, notification);
    }

    private void stopCurrentService(int startId) {
        Log.v(Utils.logTag, "Calling stop service");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.deleteNotificationChannel(channelID);
        }

        stopForeground(true);
        stopSelf(startId);
    }

    private class DownloadTask extends AsyncTask<Void, Void, Void> {
        Intent intent;
        int startId;

        private DownloadTask(Intent intent, int startId) {
            this.intent = intent;
            this.startId = startId;
        }

        @Override
        protected Void doInBackground(Void... voids) {
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
                return null;
            }
            String outputFileName = sourceUrl.substring(sourceUrl.lastIndexOf('/')+1);

            startInForeground(outputFileName);
            try {
                Log.v(Utils.logTag, "before connection created ");
                URL url = new URL(sourceUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                Log.v(Utils.logTag, "connection created");
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    bundle.putInt(Utils.IntentAndBundleKeys.downloadStatusKey, Utils.DownloadStatuses.CONNECTION_FAILED);
                    resultReceiver.send(Activity.RESULT_OK, bundle);
                    return null;
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
                    return null;
                }

                if (!outputDirectory.exists()) {
                    boolean directoryCreated = outputDirectory.mkdir();
                    if (!directoryCreated) {
                        bundle.putInt(Utils.IntentAndBundleKeys.downloadStatusKey, Utils.DownloadStatuses.OUTPUT_DIR_CREATION_FAILED);
                        resultReceiver.send(Activity.RESULT_OK, bundle);
                        return null;
                    }
                }

                outputFile = new File(outputDirectory, outputFileName);
                if (!outputFile.exists()) {
                    boolean fileCreated = outputFile.createNewFile();
                    if (!fileCreated) {
                        bundle.putInt(Utils.IntentAndBundleKeys.downloadStatusKey, Utils.DownloadStatuses.OUTPUT_FILE_CREATION_FAILED);
                        resultReceiver.send(Activity.RESULT_OK, bundle);
                        return null;
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

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            stopCurrentService(startId);
        }
    }
}