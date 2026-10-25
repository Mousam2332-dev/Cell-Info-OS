package com.example.emptyactivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.CellInfo;
import android.telephony.TelephonyManager;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQ_CODE = 100;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                fetchAndDisplayData();
                handler.postDelayed(this, 2000);
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.READ_PHONE_STATE
            }, PERMISSION_REQ_CODE);
        } else {
            startRefreshing();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startRefreshing();
        }
    }

    private void startRefreshing() {
        handler.removeCallbacks(refreshRunnable);
        handler.post(refreshRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(refreshRunnable);
    }

    @SuppressWarnings("MissingPermission")
    private void fetchAndDisplayData() {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        List<CellInfo> allCells = tm.getAllCellInfo();
        
        if (allCells == null) return;

        int registeredSimCount = 1;
        
        for (CellInfo info : allCells) {
            if (info.isRegistered()) {
                String prefix = registeredSimCount == 1 ? "sim1_" : "sim2_";
                updateUiForSim(prefix, info, registeredSimCount);
                registeredSimCount++;
                if (registeredSimCount > 2) break; // Support dual SIM display maximum
            }
        }
    }

    private void updateUiForSim(String prefix, CellInfo info, int simIndex) {
        TextView title = findViewById(getResources().getIdentifier("carrier_title_" + simIndex, "id", getPackageName()));
        TextView cellId = findViewById(getResources().getIdentifier(prefix + "cellid", "id", getPackageName()));
        TextView arfcn = findViewById(getResources().getIdentifier(prefix + "arfcn", "id", getPackageName()));
        TextView tac = findViewById(getResources().getIdentifier(prefix + "tac", "id", getPackageName()));
        TextView pci = findViewById(getResources().getIdentifier(prefix + "pci", "id", getPackageName()));
        TextView bw = findViewById(getResources().getIdentifier(prefix + "bw", "id", getPackageName()));
        TextView rsrp = findViewById(getResources().getIdentifier(prefix + "rsrp", "id", getPackageName()));
        TextView rsrq = findViewById(getResources().getIdentifier(prefix + "rsrq", "id", getPackageName()));
        TextView sinr = findViewById(getResources().getIdentifier(prefix + "sinr", "id", getPackageName()));

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && info instanceof android.telephony.CellInfoNr) {
            android.telephony.CellInfoNr nrInfo = (android.telephony.CellInfoNr) info;
            android.telephony.CellIdentityNr id = (android.telephony.CellIdentityNr) nrInfo.getCellIdentity();
            android.telephony.CellSignalStrengthNr sig = (android.telephony.CellSignalStrengthNr) nrInfo.getCellSignalStrength();

            CharSequence opName = id.getOperatorAlphaLong();
            title.setText((opName != null ? opName.toString() : "Unknown") + " • 5G SA");
            
            cellId.setText(id.getNci() != android.telephony.CellInfo.UNAVAILABLE_LONG ? String.valueOf(id.getNci()) : "-");
            arfcn.setText(id.getNrarfcn() != android.telephony.CellInfo.UNAVAILABLE ? String.valueOf(id.getNrarfcn()) : "-");
            tac.setText(id.getTac() != android.telephony.CellInfo.UNAVAILABLE ? String.valueOf(id.getTac()) : "-");
            pci.setText(id.getPci() != android.telephony.CellInfo.UNAVAILABLE ? String.valueOf(id.getPci()) : "-");
            
            rsrp.setText(String.valueOf(sig.getDbm()));
            rsrq.setText("-"); // Derived differently on NR depending on device capability
            sinr.setText("-"); 
            bw.setText("-");
            
        } else if (info instanceof android.telephony.CellInfoLte) {
            android.telephony.CellInfoLte lteInfo = (android.telephony.CellInfoLte) info;
            android.telephony.CellIdentityLte id = lteInfo.getCellIdentity();
            android.telephony.CellSignalStrengthLte sig = lteInfo.getCellSignalStrength();

            CharSequence opName = id.getOperatorAlphaLong();
            title.setText((opName != null ? opName.toString() : "Unknown") + " • 4G LTE");
            
            cellId.setText(id.getCi() != android.telephony.CellInfo.UNAVAILABLE ? String.valueOf(id.getCi()) : "-");
            arfcn.setText(id.getEarfcn() != android.telephony.CellInfo.UNAVAILABLE ? String.valueOf(id.getEarfcn()) : "-");
            tac.setText(id.getTac() != android.telephony.CellInfo.UNAVAILABLE ? String.valueOf(id.getTac()) : "-");
            pci.setText(id.getPci() != android.telephony.CellInfo.UNAVAILABLE ? String.valueOf(id.getPci()) : "-");
            bw.setText(id.getBandwidth() != android.telephony.CellInfo.UNAVAILABLE ? (id.getBandwidth() / 1000) + " MHz" : "-");

            rsrp.setText(String.valueOf(sig.getRsrp()));
            rsrq.setText(String.valueOf(sig.getRsrq()));
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                sinr.setText(String.valueOf(sig.getRssnr()));
            } else {
                sinr.setText("-");
            }
        }
    }
}
