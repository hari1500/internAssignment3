package com.example.assignment3;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private static final int DOWNLOAD_JOB_ID = 5080;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private TextView textViewDownloadPercent;
    private Button buttonDownload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewDownloadPercent = findViewById(R.id.textViewDownloadPercent);
        buttonDownload = findViewById(R.id.buttonDownload);
        checkPermissions();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(Utils.logTag, "Destroying MainActivity");
    }

    public void onClickDownloadButton(View v) {
        textViewDownloadPercent.setText(Utils.TextViewStrings.REQUESTED);
        buttonDownload.setEnabled(false);

        Intent intent = new Intent();
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
        DownloadService.enqueueWork(getApplicationContext(), DownloadService.class, DOWNLOAD_JOB_ID, intent);
    }

    private void updateUI(int status, int percent) {
        switch (status) {
            case Utils.DownloadStatuses.PENDING:
                textViewDownloadPercent.setText(Utils.TextViewStrings.PENDING);
                break;
            case Utils.DownloadStatuses.ONGOING:
                textViewDownloadPercent.setText(String.format("Progress: %s/100", percent));
                break;
            case Utils.DownloadStatuses.COMPLETED:
                textViewDownloadPercent.setText(Utils.TextViewStrings.COMPLETED.concat(Utils.downloadDirectoryPath));
                break;
            case Utils.DownloadStatuses.CONNECTION_FAILED:
                textViewDownloadPercent.setText(Utils.TextViewStrings.CONNECTION_FAILED);
                break;
            case Utils.DownloadStatuses.SD_CARD_NOT_EXISTS:
                textViewDownloadPercent.setText(Utils.TextViewStrings.SD_CARD_NOT_EXISTS);
                break;
            case Utils.DownloadStatuses.OUTPUT_DIR_CREATION_FAILED:
                textViewDownloadPercent.setText(Utils.TextViewStrings.OUTPUT_DIR_CREATION_FAILED);
                break;
            case Utils.DownloadStatuses.OUTPUT_FILE_CREATION_FAILED:
                textViewDownloadPercent.setText(Utils.TextViewStrings.OUTPUT_FILE_CREATION_FAILED);
                break;
            case Utils.DownloadStatuses.IMPROPER_URL:
                textViewDownloadPercent.setText(Utils.TextViewStrings.IMPROPER_URL);
                break;
            case Utils.DownloadStatuses.FAILED:
                textViewDownloadPercent.setText(Utils.TextViewStrings.FAILED);
                break;
        }

        buttonDownload.setEnabled(status >= Utils.DownloadStatuses.COMPLETED);
    }

    void checkPermissions() {
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
                textViewDownloadPercent.setText(Utils.TextViewStrings.PROVIDE_STORAGE_ACCESS);
            }
        }
    }
}
