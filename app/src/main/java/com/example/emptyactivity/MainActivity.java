package com.example.emptyactivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.*;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private TelephonyManager telephonyManager;
    private LinearLayout cardsContainer;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_main);

        cardsContainer = findViewById(R.id.cards_container);
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

    private int extractTimingAdvance(String cellString) {
        try {
            Pattern pattern = Pattern.compile("timingAdvance\\s*=\\s*(-?\\d+)");
            Matcher matcher = pattern.matcher(cellString);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        } catch (Exception ignored) {}
        return -1;
    }

    private String formatDistance(int ta, boolean is5gHighBand) {
        if (ta == -1 || ta == CellInfo.UNAVAILABLE || ta == Integer.MAX_VALUE || ta == 2147483647) {
            return "N/A";
        }
        int metersPerTa = is5gHighBand ? 39 : 78;
        int totalMeters = ta * metersPerTa;
        if (totalMeters >= 1000) {
            return String.format("%.2f km", totalMeters / 1000.0);
        }
        return totalMeters + " m";
    }

    private int getRsrpColor(int rsrp) {
        if (rsrp == CellInfo.UNAVAILABLE || rsrp == 0) return Color.parseColor("#18181F");
        if (rsrp >= -80) return Color.parseColor("#1B3B2B"); // Excellent (Dark Emerald)
        if (rsrp >= -90) return Color.parseColor("#3B3B1B"); // Good (Dark Olive)
        if (rsrp >= -100) return Color.parseColor("#4D3B14"); // Fair (Dark Amber)
        if (rsrp >= -110) return Color.parseColor("#4D2614"); // Poor (Dark Orange)
        return Color.parseColor("#4D1414"); // Bad (Dark Maroon)
    }

    private void updateCellMetrics() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            try {
                telephonyManager.requestCellInfoUpdate(getMainExecutor(), new TelephonyManager.CellInfoCallback() {
                    @Override
                    public void onCellInfo(List<CellInfo> cellInfo) {}
                });
            } catch (Exception ignored) {}
        }

        cardsContainer.removeAllViews();

        TextView header = new TextView(this);
        header.setText("Cellular Monitor");
        header.setTextColor(Color.WHITE);
        header.setTextSize(22);
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        header.setPadding(0, 0, 0, 24);
        cardsContainer.addView(header);

        try {
            List<CellInfo> allCells = telephonyManager.getAllCellInfo();
            if (allCells == null || allCells.isEmpty()) {
                TextView empty = new TextView(this);
                empty.setText("No cell info available. Ensure GPS is ON.");
                empty.setTextColor(Color.parseColor("#00FF66"));
                cardsContainer.addView(empty);
                return;
            }

            for (CellInfo cell : allCells) {
                if (!cell.isRegistered()) continue;

                String cellStr = cell.toString();
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(24, 24, 24, 24);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 0, 0, 16);
                card.setLayoutParams(params);

                String operatorName = telephonyManager.getNetworkOperatorName();
                if (operatorName.isEmpty()) operatorName = "SIM Cell";

                if (cell instanceof CellInfoLte) {
                    CellInfoLte lte = (CellInfoLte) cell;
                    CellIdentityLte identity = lte.getCellIdentity();
                    int ci = identity.getCi();
                    if (ci == 268435455 || ci == -1) continue;

                    CellSignalStrengthLte signal = lte.getCellSignalStrength();
                    int enbId = ci >> 8;
                    int sectorId = ci & 0xFF;
                    int rsrp = signal.getRsrp();
                    int rsrq = signal.getRsrq();
                    int rssnr = signal.getRssnr();
                    int ta = signal.getTimingAdvance();

                    card.setBackgroundColor(getRsrpColor(rsrp));

                    card.addView(createRow(operatorName.toUpperCase(), identity.getMcc() + " " + identity.getMnc(), true));
                    card.addView(createRow("4G | Band " + identity.getBands(), "PCI: " + identity.getPci(), false));
                    card.addView(createRow("CI", String.valueOf(ci), false));
                    card.addView(createRow("eNB:CI", enbId + ":" + sectorId, false));
                    card.addView(createRow("TAC", String.valueOf(identity.getTac()), false));
                    card.addView(createRow("PCI", String.valueOf(identity.getPci()), false));
                    card.addView(createRow("ARFCN", String.valueOf(identity.getEarfcn()), false));
                    card.addView(createRow("S-RSRP", rsrp + " dBm", false));
                    card.addView(createRow("S-RSRQ", rsrq + " dB", false));
                    card.addView(createRow("S-SNR", (rssnr != CellInfo.UNAVAILABLE ? rssnr + " dB" : "N/A"), false));
                    card.addView(createRow("TA", ta + " | " + formatDistance(ta, false), false));

                    cardsContainer.addView(card);

                } else if (cell instanceof CellInfoNr) {
                    CellInfoNr nr = (CellInfoNr) cell;
                    CellIdentityNr identity = (CellIdentityNr) nr.getCellIdentity();
                    CellSignalStrengthNr signal = (CellSignalStrengthNr) nr.getCellSignalStrength();

                    int ssRsrp = signal.getSsRsrp();
                    int ssRsrq = signal.getSsRsrq();
                    int ssSinr = signal.getSsSinr();
                    int ta = extractTimingAdvance(cellStr);

                    int arfcn = identity.getNrarfcn();
                    boolean isHighBand = (arfcn >= 620000 && arfcn <= 680000) || 
                                         (arfcn >= 499200 && arfcn <= 537999);

                    card.setBackgroundColor(getRsrpColor(ssRsrp));

                    card.addView(createRow(operatorName.toUpperCase(), identity.getMccString() + " " + identity.getMncString(), true));
                    card.addView(createRow("5G | NR ARFCN " + arfcn, "PCI: " + identity.getPci(), false));
                    card.addView(createRow("NCI", String.valueOf(identity.getNci()), false));
                    card.addView(createRow("TAC", String.valueOf(identity.getTac()), false));
                    card.addView(createRow("PCI", String.valueOf(identity.getPci()), false));
                    card.addView(createRow("ARFCN", String.valueOf(arfcn), false));
                    card.addView(createRow("S-RSRP", ssRsrp + " dBm", false));
                    card.addView(createRow("S-RSRQ", ssRsrq + " dB", false));
                    card.addView(createRow("S-SNR", ssSinr + " dB", false));
                    card.addView(createRow("TA", ta + " | " + formatDistance(ta, isHighBand), false));

                    cardsContainer.addView(card);
                }
            }
        } catch (SecurityException ignored) {}
    }

    private LinearLayout createRow(String label, String value, boolean isHeader) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 4, 0, 4);

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(isHeader ? Color.WHITE : Color.parseColor("#9AA0A6"));
        lbl.setTextSize(isHeader ? 18 : 15);
        lbl.setTypeface(null, isHeader ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        lbl.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView val = new TextView(this);
        val.setText(value);
        val.setTextColor(Color.WHITE);
        val.setTextSize(isHeader ? 16 : 15);
        val.setGravity(Gravity.END);
        val.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

        row.addView(lbl);
        row.addView(val);
        return row;
    }
}
