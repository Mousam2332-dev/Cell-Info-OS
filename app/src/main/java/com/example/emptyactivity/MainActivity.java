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
                updateCellMetrics();
                handler.postDelayed(this, 2000);
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

    private void updateCellMetrics() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            infoTextView.setText("Permission required for cell telemetry.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== REAL-TIME TELEMETRY ===\n\n");

        try {
            List<CellInfo> allCells = telephonyManager.getAllCellInfo();
            if (allCells == null || allCells.isEmpty()) {
                sb.append("No cell info available. Turn on Location/GPS.\n");
            } else {
                for (int i = 0; i < allCells.size(); i++) {
                    CellInfo cell = allCells.get(i);
                    sb.append("--- Tower [").append(i + 1).append("] (").append(cell.getClass().getSimpleName()).append(") ---\n");
                    sb.append("Registered: ").append(cell.isRegistered()).append("\n");

                    if (cell instanceof CellInfoLte) {
                        CellInfoLte lte = (CellInfoLte) cell;
                        CellIdentityLte identity = lte.getCellIdentity();
                        CellSignalStrengthLte signal = lte.getCellSignalStrength();
                        sb.append("PCI: ").append(identity.getPci()).append(" | EARFCN: ").append(identity.getEarfcn()).append("\n");
                        sb.append("TAC: ").append(identity.getTac()).append(" | CI: ").append(identity.getCi()).append("\n");
                        sb.append("RSRP: ").append(signal.getRsrp()).append(" dBm\n");
                        sb.append("RSRQ: ").append(signal.getRsrq()).append(" dB\n");
                        sb.append("Timing Advance: ").append(signal.getTimingAdvance()).append("\n");
                    } else if (cell instanceof CellInfoGsm) {
                        CellInfoGsm gsm = (CellInfoGsm) cell;
                        CellIdentityGsm identity = gsm.getCellIdentity();
                        CellSignalStrengthGsm signal = gsm.getCellSignalStrength();
                        sb.append("BSIC: ").append(identity.getBsic()).append(" | ARFCN: ").append(identity.getArfcn()).append("\n");
                        sb.append("LAC: ").append(identity.getLac()).append(" | CID: ").append(identity.getCid()).append("\n");
                        sb.append("RSSI: ").append(signal.getDbm()).append(" dBm\n");
                    } else if (cell instanceof CellInfoWcdma) {
                        CellInfoWcdma wcdma = (CellInfoWcdma) cell;
                        CellIdentityWcdma identity = wcdma.getCellIdentity();
                        CellSignalStrengthWcdma signal = wcdma.getCellSignalStrength();
                        sb.append("PSC: ").append(identity.getPsc()).append(" | UARFCN: ").append(identity.getUarfcn()).append("\n");
                        sb.append("LAC: ").append(identity.getLac()).append(" | CID: ").append(identity.getCid()).append("\n");
                        sb.append("RSSI: ").append(signal.getDbm()).append(" dBm\n");
                    }
                    sb.append("\n");
                }
            }
        } catch (SecurityException e) {
            sb.append("Security Exception reading cell info.\n");
        }

        infoTextView.setText(sb.toString());
    }
}
