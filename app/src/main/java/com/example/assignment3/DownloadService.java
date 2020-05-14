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

    public boolean isTaskCompleted() {
        return downloadTask.getIsTaskCompleted();
    }

    public void downloadFile() {
        downloadTask.execute();
    }

    private static class DownloadTask extends AsyncTask<Void, Void, Void> {
        File outputDirectory = null, outputFile = null;
        int progressPercent = 0;
        boolean isTaskCompleted = false;

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                URL url = new URL(Utils.sourceUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    Log.v(Utils.logTag, "Connection Failed");
                    return null;
                }

                if (isSDCardPresent()) {
                    outputDirectory = new File(
                            Environment.getExternalStorageDirectory().getAbsolutePath(),
                            Utils.downloadDirectoryPath
                    );
                } else {
                    Log.v(Utils.logTag, "SD card not present");
                    return null;
                }

                if (!outputDirectory.exists()) {
                    boolean directoryCreated = outputDirectory.mkdir();
                    if (!directoryCreated) {
                        Log.v(Utils.logTag, "Output Directory creation failed");
                        return null;
                    } else {
                        Log.v(Utils.logTag, "Output Directory created");
                    }
                } else {
                    Log.v(Utils.logTag, "Output Directory exists");
                }

                String outputFileName = Utils.sourceUrl.substring(Utils.sourceUrl.lastIndexOf('/')+1);
                outputFile = new File(outputDirectory, outputFileName);
                if (!outputFile.exists()) {
                    boolean fileCreated = outputFile.createNewFile();
                    if (!fileCreated) {
                        Log.v(Utils.logTag, "Output File creation failed");
                        return null;
                    } else {
                        Log.v(Utils.logTag, outputFile.getAbsolutePath());
                    }
                } else {
                    Log.v(Utils.logTag, "Output File exists");
                }

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
                isTaskCompleted = true;

                Log.v(Utils.logTag, "Download Completed");
            } catch (IOException e) {
                e.printStackTrace();
                outputFile = null;
                Log.v(Utils.logTag, e.toString());
            }
            return null;
        }

        private boolean isSDCardPresent() {
            return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        }

        int getProgressPercent() {
            return progressPercent;
        }

        boolean getIsTaskCompleted() {
            return isTaskCompleted;
        }
    }
}
