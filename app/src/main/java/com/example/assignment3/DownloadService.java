package com.example.assignment3;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadService extends Service {
    final IBinder binder = new LocalBinder();
    DownloadTask downloadTask = new DownloadTask();

    class LocalBinder extends Binder {
        DownloadService getService() {
            return DownloadService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public int getProgressPercent() {
        return downloadTask.getProgressPercent();
    }

    public int getDownloadStatus() {
        return downloadTask.getDownloadStatus();
    }

    public void downloadFile() {
        downloadTask.execute();
    }

    private static class DownloadTask extends AsyncTask<Void, Void, Void> {
        File outputDirectory = null, outputFile = null;
        int progressPercent = 0;
        int downloadStatus = Utils.DownloadStatuses.PENDING;

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                URL url = new URL(Utils.sourceUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    downloadStatus = Utils.DownloadStatuses.CONNECTION_FAILED;
                    return null;
                }

                if (isSDCardPresent()) {
                    outputDirectory = new File(
                            Environment.getExternalStorageDirectory().getAbsolutePath(),
                            Utils.downloadDirectoryPath
                    );
                } else {
                    downloadStatus = Utils.DownloadStatuses.SD_CARD_NOT_EXISTS;
                    return null;
                }

                if (!outputDirectory.exists()) {
                    boolean directoryCreated = outputDirectory.mkdir();
                    if (!directoryCreated) {
                        downloadStatus = Utils.DownloadStatuses.OUTPUT_DIR_CREATION_FAILED;
                        return null;
                    }
                }

                String outputFileName = Utils.sourceUrl.substring(Utils.sourceUrl.lastIndexOf('/')+1);
                outputFile = new File(outputDirectory, outputFileName);
                if (!outputFile.exists()) {
                    boolean fileCreated = outputFile.createNewFile();
                    if (!fileCreated) {
                        downloadStatus = Utils.DownloadStatuses.OUTPUT_FILE_CREATION_FAILED;
                        return null;
                    }
                }

                downloadStatus = Utils.DownloadStatuses.ONGOING;
                FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
                InputStream inputStream = connection.getInputStream();
                byte[] buffer = new byte[1024];
                int nBytesRead, nBytesCumm = 0, fileSize = connection.getContentLength();
                while ((nBytesRead = inputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, nBytesRead);
                    nBytesCumm += nBytesRead;
                    progressPercent = ((nBytesCumm * 100) / fileSize);
                }

                fileOutputStream.close();
                inputStream.close();
                connection.disconnect();
                downloadStatus = Utils.DownloadStatuses.COMPLETED;

                Log.v(Utils.logTag, "doInBackground Download Completed");
            } catch (IOException e) {
                e.printStackTrace();
                outputFile = null;
                Log.v(Utils.logTag, e.toString());
                downloadStatus = Utils.DownloadStatuses.FAILED;
            }
            return null;
        }

        private boolean isSDCardPresent() {
            return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        }

        int getProgressPercent() {
            return progressPercent;
        }

        int getDownloadStatus() {
            return downloadStatus;
        }
    }
}
