package com.example.assignment3;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {
    TextView textViewDownloadPercent;
    Button buttonDownload;

    DownloadService downloadService;
    boolean bound = false;

    final static int UPDATE_TEXT_VIEW = 0;
    final static int UPDATE_BUTTON = 1;
    final static String BUTTON_ENABLE = "ENABLE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewDownloadPercent = findViewById(R.id.textViewDownloadPercent);
        buttonDownload = findViewById(R.id.buttonDownload);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (bound) {
            unbindService(connection);
        }
    }

    ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            DownloadService.LocalBinder binder = (DownloadService.LocalBinder) service;
            downloadService = binder.getService();
            bound = true;

            buttonDownload.setEnabled(false);
            downloadService.downloadFile();
            getProgress();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bound = false;
            buttonDownload.setEnabled(true);
        }
    };

    public void onClickDownloadButton(View v) {
        Intent intent = new Intent(this, DownloadService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);

        textViewDownloadPercent.setText(Utils.TextViewStrings.REQUESTED);
//        Log.v(Utils.logTag, "main activity"+Thread.currentThread().getId());
    }

    void updateOnUIThread(final int updateIndex, final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (updateIndex) {
                    case UPDATE_TEXT_VIEW:
                        textViewDownloadPercent.setText(text);
                        break;
                    case UPDATE_BUTTON:
                        buttonDownload.setEnabled(text.equals(BUTTON_ENABLE));
                        break;
                }
            }
        });
    }

    void updateTextBasedOnStatus(int status) {
        switch (status) {
            case Utils.DownloadStatuses.COMPLETED:
                updateOnUIThread(UPDATE_TEXT_VIEW, Utils.TextViewStrings.COMPLETED);
                break;
            case Utils.DownloadStatuses.CONNECTION_FAILED:
                updateOnUIThread(UPDATE_TEXT_VIEW, Utils.TextViewStrings.CONNECTION_FAILED);
                break;
            case Utils.DownloadStatuses.SD_CARD_NOT_EXISTS:
                updateOnUIThread(UPDATE_TEXT_VIEW, Utils.TextViewStrings.SD_CARD_NOT_EXISTS);
                break;
            case Utils.DownloadStatuses.OUTPUT_DIR_CREATION_FAILED:
                updateOnUIThread(UPDATE_TEXT_VIEW, Utils.TextViewStrings.OUTPUT_DIR_CREATION_FAILED);
                break;
            case Utils.DownloadStatuses.OUTPUT_FILE_CREATION_FAILED:
                updateOnUIThread(UPDATE_TEXT_VIEW, Utils.TextViewStrings.OUTPUT_FILE_CREATION_FAILED);
                break;
            case Utils.DownloadStatuses.FAILED:
                updateOnUIThread(UPDATE_TEXT_VIEW, Utils.TextViewStrings.FAILED);
                break;
        }
    }

    void getProgress() {
        Thread thread = new Thread(){
            @Override
            public void run() {
                super.run();
//                Log.v(Utils.logTag, "getProgress "+Thread.currentThread().getId());
                try {
                    int downloadStatus;
                    while ((downloadStatus = downloadService.getDownloadStatus()) <= 0) {
                        if (downloadStatus == Utils.DownloadStatuses.ONGOING) {
                            int progressPercent = downloadService.getProgressPercent();
                            Log.v(Utils.logTag, "progress percent "+progressPercent);
                            if (bound) {
                                updateOnUIThread(
                                        UPDATE_TEXT_VIEW,
                                        String.format("Progress: %s/100", progressPercent)
                                );
                            }
                        } else if (downloadStatus == Utils.DownloadStatuses.PENDING) {
                            if (bound) {
                                updateOnUIThread(UPDATE_TEXT_VIEW, Utils.TextViewStrings.PENDING);
                            }
                        }
                        Thread.sleep(Utils.sleepTime);
                    }

                    if (bound) {
                        unbindService(connection);
                        bound = false;
                        updateOnUIThread(UPDATE_BUTTON, BUTTON_ENABLE);
                    }

                    updateTextBasedOnStatus(downloadService.getDownloadStatus());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }
}
