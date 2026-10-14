package com.example.emptyactivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.*;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private TelephonyManager telephonyManager;
    private TextView infoTextView;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_main);

        infoTextView = findViewById(R.id.txt_cell_info);
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE
            }, 101);
        }

        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                updateRawMetrics();
                handler.postDelayed(this, 1000); // Live refresh every 1 second
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(refreshRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(refreshRunnable);
    }

    private void updateRawMetrics() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            infoTextView.setText("Permission required for cell telemetry.\nPlease grant Location permission.");
            return;
        }

        // Force modem refresh on Android 10+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            try {
                telephonyManager.requestCellInfoUpdate(getMainExecutor(), new TelephonyManager.CellInfoCallback() {
                    @Override
                    public void onCellInfo(List<CellInfo> cellInfo) {}
                });
            } catch (Exception ignored) {}
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== RAW MODEM DUMP ===\n\n");

        try {
            // 1. Dump all CellInfo objects raw toString()
            List<CellInfo> allCells = telephonyManager.getAllCellInfo();
            if (allCells == null || allCells.isEmpty()) {
                sb.append("getAllCellInfo() is EMPTY. Turn on GPS/Location.\n\n");
            } else {
                sb.append("--- getAllCellInfo() ---\n");
                for (int i = 0; i < allCells.size(); i++) {
                    sb.append("[").append(i).append("] ").append(allCells.get(i).toString()).append("\n\n");
                }
            }

            // 2. Dump SignalStrength object raw toString()
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                SignalStrength ss = telephonyManager.getSignalStrength();
                if (ss != null) {
                    sb.append("--- getSignalStrength() ---\n");
                    sb.append(ss.toString()).append("\n");
                }
            }

        } catch (SecurityException e) {
            sb.append("Security Exception reading raw info.\n");
        }

        infoTextView.setText(sb.toString());
    }
}
