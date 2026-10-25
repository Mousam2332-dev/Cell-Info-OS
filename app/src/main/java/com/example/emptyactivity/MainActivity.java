package com.example.emptyactivity; // Keep your actual package name

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.CellInfo;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView rawDataText;
    private static final int PERMISSION_REQ_CODE = 100;
    
    // Handler to manage the 2-second refresh loop
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        rawDataText = findViewById(R.id.rawText);
        
        // Define the repeating task
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                fetchAndDisplayData();
                handler.postDelayed(this, 2000); // 2000 ms = 2 seconds
            }
        };

        checkPermissionsAndRun();
    }

    private void checkPermissionsAndRun() {
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
        } else {
            rawDataText.setText("Please grant Location and Phone permissions to scan networks.");
        }
    }

    private void startRefreshing() {
        handler.removeCallbacks(refreshRunnable); // Clear any existing loops
        handler.post(refreshRunnable); // Start the refresh loop
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(refreshRunnable); // Stop refreshing when app closes
    }

    @SuppressWarnings("MissingPermission")
    private void fetchAndDisplayData() {
        TelephonyManager defaultTm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        SubscriptionManager sm = (SubscriptionManager) getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        StringBuilder rawDump = new StringBuilder();

        try {
            List<SubscriptionInfo> subInfoList = sm.getActiveSubscriptionInfoList();
            rawDump.append("========== SUBSCRIPTIONS ==========\n");
            
            if (subInfoList != null) {
                for (SubscriptionInfo si : subInfoList) {
                    rawDump.append("SLOT ").append(si.getSimSlotIndex()).append(" | ").append(si.getCarrierName()).append("\n");
                    
                    TelephonyManager simTm = defaultTm.createForSubscriptionId(si.getSubscriptionId());
                    rawDump.append("Operator: ").append(simTm.getNetworkOperator()).append("\n");
                    rawDump.append("Network Type: ").append(simTm.getDataNetworkType()).append("\n");
                    
                    ServiceState ss = simTm.getServiceState();
                    rawDump.append("\n--- SERVICE STATE ---\n");
                    rawDump.append(ss != null ? ss.toString() : "null").append("\n");
                    
                    SignalStrength sig = simTm.getSignalStrength();
                    rawDump.append("\n--- SIGNAL STRENGTH ---\n");
                    rawDump.append(sig != null ? sig.toString() : "null").append("\n");
                    rawDump.append("-----------------------------------\n\n");
                }
            } else {
                rawDump.append("No active subscriptions found.\n\n");
            }

            rawDump.append("========== ALL CELL INFO ==========\n");
            List<CellInfo> allCells = defaultTm.getAllCellInfo();
            
            if (allCells != null) {
                for (int i = 0; i < allCells.size(); i++) {
                    CellInfo info = allCells.get(i);
                    String regStatus = info.isRegistered() ? "[REGISTERED]" : "[NEIGHBOR]";
                    
                    rawDump.append(regStatus).append(" Cell ").append(i + 1).append(":\n");
                    rawDump.append(info.toString().replaceAll(", ", "\n  ")).append("\n\n");
                }
            } else {
                rawDump.append("getAllCellInfo() returned null (Check Location permissions/GPS state).\n");
            }
        } catch (Exception e) {
            rawDump.append("CRASH: ").append(e.toString());
        }

        rawDataText.setText(rawDump.toString());
    }
}
