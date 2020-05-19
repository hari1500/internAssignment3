package com.example.assignment3;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.JobIntentService;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private static final int DOWNLOAD_JOB_ID = 5080;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private TextView textViewDownloadMessage;
    private Button buttonDownload;
    private ProgressBar progressBarDownloadPercent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonDownload = findViewById(R.id.buttonDownload);
        textViewDownloadMessage = findViewById(R.id.textViewDownloadMessage);
        progressBarDownloadPercent = findViewById(R.id.progressBarDownloadPercent);
        checkPermissions();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(Utils.logTag, "Destroying MainActivity");
    }

    public void onClickDownloadButton(View v) {
        textViewDownloadMessage.setText(Utils.TextViewMessages.REQUESTED);
        buttonDownload.setEnabled(false);

        Intent intent = new Intent(this, DownloadServiceForeground.class);
//        Intent intent = new Intent();
        intent.putExtra(Utils.IntentAndBundleKeys.sourceUrlKey, Utils.sourceUrl);
        intent.putExtra(Utils.IntentAndBundleKeys.resultReceiverKey, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);

                if (resultCode == Activity.RESULT_OK) {
                    updateUI(
                            resultData.getInt(Utils.IntentAndBundleKeys.downloadStatusKey),
                            resultData.getInt(Utils.IntentAndBundleKeys.progressPercentKey, 0)
                    );
                }
            }
        });
//        DownloadService.enqueueWork(getApplicationContext(), DownloadService.class, DOWNLOAD_JOB_ID, intent);
        ContextCompat.startForegroundService(this, intent);
    }

    private void updateUI(int status, int percent) {
        switch (status) {
            case Utils.DownloadStatuses.PENDING:
                textViewDownloadMessage.setText(Utils.TextViewMessages.PENDING);
                break;
            case Utils.DownloadStatuses.ONGOING:
                textViewDownloadMessage.setText(String.format("Progress: %s/100", percent));
                break;
            case Utils.DownloadStatuses.COMPLETED:
                textViewDownloadMessage.setText(Utils.TextViewMessages.COMPLETED.concat(Utils.downloadDirectoryPath));
                break;
            case Utils.DownloadStatuses.CONNECTION_FAILED:
                textViewDownloadMessage.setText(Utils.TextViewMessages.CONNECTION_FAILED);
                break;
            case Utils.DownloadStatuses.SD_CARD_NOT_EXISTS:
                textViewDownloadMessage.setText(Utils.TextViewMessages.SD_CARD_NOT_EXISTS);
                break;
            case Utils.DownloadStatuses.OUTPUT_DIR_CREATION_FAILED:
                textViewDownloadMessage.setText(Utils.TextViewMessages.OUTPUT_DIR_CREATION_FAILED);
                break;
            case Utils.DownloadStatuses.OUTPUT_FILE_CREATION_FAILED:
                textViewDownloadMessage.setText(Utils.TextViewMessages.OUTPUT_FILE_CREATION_FAILED);
                break;
            case Utils.DownloadStatuses.IMPROPER_URL:
                textViewDownloadMessage.setText(Utils.TextViewMessages.IMPROPER_URL);
                break;
            case Utils.DownloadStatuses.FAILED:
                textViewDownloadMessage.setText(Utils.TextViewMessages.FAILED);
                break;
        }

        buttonDownload.setEnabled(status >= Utils.DownloadStatuses.COMPLETED);
        if (status == Utils.DownloadStatuses.ONGOING) {
            progressBarDownloadPercent.setVisibility(View.VISIBLE);
            progressBarDownloadPercent.setProgress(percent);
        } else {
            progressBarDownloadPercent.setVisibility(View.INVISIBLE);
        }
    }

    private void checkPermissions() {
        String[] requiredPermissions = new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        boolean hasAllPermissions = true;
        for (String per : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, per) == PackageManager.PERMISSION_DENIED) {
                hasAllPermissions = false;
                break;
            }
        }

        if (!hasAllPermissions) {
            ActivityCompat.requestPermissions(this, requiredPermissions, PERMISSIONS_REQUEST_CODE);
        } else {
            buttonDownload.setEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            boolean grantedAllPermissions = true;
            for(int per : grantResults) {
                if (per != PackageManager.PERMISSION_GRANTED) {
                    grantedAllPermissions = false;
                    break;
                }
            }
            if (grantedAllPermissions) {
                buttonDownload.setEnabled(true);
            } else {
                buttonDownload.setEnabled(false);
                textViewDownloadMessage.setText(Utils.TextViewMessages.PROVIDE_STORAGE_ACCESS);
            }
        }
    }
}
