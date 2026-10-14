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
                handler.postDelayed(this, 2000); // Refreshes every 2 seconds
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(refreshRunnable); // Starts the refresh loop when app opens/resumes
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(refreshRunnable); // Stops loop when app is in background to save battery
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
                for (int i = 0; i < allCells.size(); i++) {
                    CellInfo cell = allCells.get(i);
                    sb.append("--- Tower [").append(i + 1).append("] (").append(cell.getClass().getSimpleName()).append(") ---\n");
                    sb.append("Registered: ").append(cell.isRegistered() ? "YES" : "NO").append("\n");

                    if (cell instanceof CellInfoLte) {
                        CellInfoLte lte = (CellInfoLte) cell;
                        CellIdentityLte identity = lte.getCellIdentity();
                        CellSignalStrengthLte signal = lte.getCellSignalStrength();

                        int ci = identity.getCi();
                        int enbId = ci != CellInfo.UNAVAILABLE ? (ci >> 8) : -1;
                        int sectorId = ci != CellInfo.UNAVAILABLE ? (ci & 0xFF) : -1;

                        sb.append("PCI: ").append(identity.getPci()).append(" | EARFCN: ").append(identity.getEarfcn()).append("\n");
                        sb.append("TAC: ").append(identity.getTac()).append("\n");
                        sb.append("Cell ID (CI): ").append(ci != CellInfo.UNAVAILABLE ? ci : "N/A").append("\n");
                        sb.append("eNB ID: ").append(enbId != -1 ? enbId : "N/A").append(" | Sector: ").append(sectorId != -1 ? sectorId : "N/A").append("\n");
                        sb.append("RSRP: ").append(signal.getRsrp()).append(" dBm\n");
                        sb.append("RSRQ: ").append(signal.getRsrq()).append(" dB\n");
                        
                        // Safe SINR check for Android 10+ (API 29+)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            int sinr = signal.getSinr();
                            sb.append("SINR: ").append(sinr != CellInfo.UNAVAILABLE ? sinr + " dB" : "N/A").append("\n");
                        }
                        
                        sb.append("Timing Advance: ").append(signal.getTimingAdvance()).append("\n");
                    } else if (cell instanceof CellInfoNr) {
                        CellInfoNr nr = (CellInfoNr) cell;
                        CellIdentityNr identity = (CellIdentityNr) nr.getCellIdentity();
                        CellSignalStrengthNr signal = (CellSignalStrengthNr) nr.getCellSignalStrength();
                        sb.append("NR PCI: ").append(identity.getPci()).append(" | ARFCN: ").append(identity.getNrarfcn()).append("\n");
                        sb.append("TAC: ").append(identity.getTac()).append(" | NCI: ").append(identity.getNci()).append("\n");
                        sb.append("SS-RSRP: ").append(signal.getSsRsrp()).append(" dBm\n");
                        sb.append("SS-RSRQ: ").append(signal.getSsRsrq()).append(" dB\n");
                        sb.append("SS-SINR: ").append(signal.getSsSinr()).append(" dB\n");
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
