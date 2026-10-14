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
                handler.postDelayed(this, 2000); // Re-triggers every 2 seconds
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(refreshRunnable); // Starts the loop immediately when app opens
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(refreshRunnable); // Stops loop when app is minimized
    }

    private void updateCellMetrics() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            infoTextView.setText("Permission required for cell telemetry.\nPlease grant Location permission.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== REAL-TIME TELEMETRY ===\n\n");

        try {
            List<CellInfo> allCells = telephonyManager.getAllCellInfo();
            if (allCells == null || allCells.isEmpty()) {
                sb.append("No cell info available.\nEnsure Location/GPS is turned ON.\n");
            } else {
                int validCount = 0;
                for (int i = 0; i < allCells.size(); i++) {
                    CellInfo cell = allCells.get(i);

                    if (cell instanceof CellInfoLte) {
                        CellInfoLte lte = (CellInfoLte) cell;
                        CellIdentityLte identity = lte.getCellIdentity();
                        int ci = identity.getCi();

                        // Filter out invalid/placeholder phantom towers (like 268435455 / 0xFFFFFFF)
                        if (ci == 268435455 || ci == -1 || ci == Integer.MAX_VALUE) {
                            continue;
                        }

                        validCount++;
                        CellSignalStrengthLte signal = lte.getCellSignalStrength();

                        int enbId = (ci != CellInfo.UNAVAILABLE) ? (ci >> 8) : -1;
                        int sectorId = (ci != CellInfo.UNAVAILABLE) ? (ci & 0xFF) : -1;

                        sb.append("--- Tower [").append(validCount).append("] (LTE) ---\n");
                        sb.append("Registered: ").append(cell.isRegistered() ? "YES" : "NO").append("\n");
                        sb.append("PCI: ").append(identity.getPci()).append(" | EARFCN: ").append(identity.getEarfcn()).append("\n");
                        sb.append("TAC: ").append(identity.getTac()).append("\n");
                        sb.append("Cell ID (CI): ").append(ci).append("\n");
                        sb.append("eNB ID: ").append(enbId != -1 ? enbId : "N/A").append(" | Sector: ").append(sectorId != -1 ? sectorId : "N/A").append("\n");
                        sb.append("RSRP: ").append(signal.getRsrp()).append(" dBm\n");
                        sb.append("RSRQ: ").append(signal.getRsrq()).append(" dB\n");

                        // Extract SINR for Android 10+ (API 29+)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            int sinr = signal.getSinr();
                            sb.append("SINR: ").append((sinr != CellInfo.UNAVAILABLE && sinr != Integer.MAX_VALUE) ? sinr + " dB" : "N/A").append("\n");
                        }

                        sb.append("Timing Advance: ").append(signal.getTimingAdvance()).append("\n\n");

                    } else if (cell instanceof CellInfoNr) {
                        validCount++;
                        CellInfoNr nr = (CellInfoNr) cell;
                        CellIdentityNr identity = (CellIdentityNr) nr.getCellIdentity();
                        CellSignalStrengthNr signal = (CellSignalStrengthNr) nr.getCellSignalStrength();

                        sb.append("--- Tower [").append(validCount).append("] (5G NR) ---\n");
                        sb.append("Registered: ").append(cell.isRegistered() ? "YES" : "NO").append("\n");
                        sb.append("NR PCI: ").append(identity.getPci()).append(" | ARFCN: ").append(identity.getNrarfcn()).append("\n");
                        sb.append("TAC: ").append(identity.getTac()).append(" | NCI: ").append(identity.getNci()).append("\n");
                        sb.append("SS-RSRP: ").append(signal.getSsRsrp()).append(" dBm\n");
                        sb.append("SS-RSRQ: ").append(signal.getSsRsrq()).append(" dB\n");
                        sb.append("SS-SINR: ").append(signal.getSsSinr()).append(" dB\n\n");
                    }
                }
                if (validCount == 0) {
                    sb.append("Scanning for active valid serving cells...\n");
                }
            }
        } catch (SecurityException e) {
            sb.append("Security Exception reading cell info.\n");
        }

        infoTextView.setText(sb.toString());
    }
}
