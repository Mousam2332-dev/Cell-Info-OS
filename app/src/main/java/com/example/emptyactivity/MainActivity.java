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
        header.setTextSize(20);
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        header.setPadding(4, 0, 0, 16);
        cardsContainer.addView(header);

        try {
            List<CellInfo> allCells = telephonyManager.getAllCellInfo();
            if (allCells == null || allCells.isEmpty()) {
                TextView empty = new TextView(this);
                empty.setText("No cell info available. Ensure GPS is ON.");
                empty.setTextColor(Color.parseColor("#00FF66"));
                empty.setPadding(4, 0, 0, 0);
                cardsContainer.addView(empty);
                return;
            }

            for (CellInfo cell : allCells) {
                if (!cell.isRegistered()) continue; // Show active serving cells for SIM 1 & SIM 2

                String cellStr = cell.toString();
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(24, 20, 24, 20);
                card.setBackgroundColor(Color.parseColor("#141418")); // Sleek NetMonster dark card background

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
                    long ci = identity.getCi();
                    if (ci == 268435455L || ci == -1) continue;

                    CellSignalStrengthLte signal = lte.getCellSignalStrength();
                    long enbId = ci >> 8;
                    long sectorId = ci & 0xFF;
                    int rsrp = signal.getRsrp();
                    int rsrq = signal.getRsrq();
                    int rssnr = signal.getRssnr();
                    int ta = signal.getTimingAdvance();

                    int band = (identity.getBands().length > 0) ? identity.getBands()[0] : 0;

                    card.addView(createTopRow(operatorName.toUpperCase(), identity.getMcc() + " " + identity.getMnc()));
                    card.addView(createBadgeRow("4G | LTE Band " + band));

                    LinearLayout grid = new LinearLayout(this);
                    grid.setOrientation(LinearLayout.VERTICAL);
                    grid.setPadding(0, 6, 0, 0);

                    grid.addView(createPairRow("CI", String.valueOf(ci), "ARFCN", String.valueOf(identity.getEarfcn())));
                    grid.addView(createPairRow("eNB:CI", enbId + ":" + sectorId, "RSRP", rsrp + " dBm"));
                    grid.addView(createPairRow("TAC", String.valueOf(identity.getTac()), "RSRQ", rsrq + " dB"));
                    grid.addView(createPairRow("PCI", String.valueOf(identity.getPci()), "SNR", (rssnr != CellInfo.UNAVAILABLE ? rssnr + " dB" : "N/A")));
                    grid.addView(createPairRow("TA", ta + " | " + formatDistance(ta, false), "BW", "10 MHz"));

                    card.addView(grid);
                    cardsContainer.addView(card);

                } else if (cell instanceof CellInfoNr) {
                    CellInfoNr nr = (CellInfoNr) cell;
                    CellIdentityNr identity = (CellIdentityNr) nr.getCellIdentity();
                    CellSignalStrengthNr signal = (CellSignalStrengthNr) nr.getCellSignalStrength();

                    long nci = identity.getNci();
                    long gnbId = nci >> 12;
                    long nrSector = nci & 0xFFF;

                    int ssRsrp = signal.getSsRsrp();
                    int ssRsrq = signal.getSsRsrq();
                    int ssSinr = signal.getSsSinr();
                    int ta = extractTimingAdvance(cellStr);

                    int arfcn = identity.getNrarfcn();
                    boolean isHighBand = (arfcn >= 620000 && arfcn <= 680000) || 
                                         (arfcn >= 499200 && arfcn <= 537999);

                    card.addView(createTopRow(operatorName.toUpperCase(), identity.getMccString() + " " + identity.getMncString()));
                    card.addView(createBadgeRow("5G | SA ARFCN " + arfcn));

                    LinearLayout grid = new LinearLayout(this);
                    grid.setOrientation(LinearLayout.VERTICAL);
                    grid.setPadding(0, 6, 0, 0);

                    grid.addView(createPairRow("NCI", String.valueOf(nci), "ARFCN", String.valueOf(arfcn)));
                    grid.addView(createPairRow("gNB:CI", gnbId + ":" + nrSector, "S-RSRP", ssRsrp + " dBm"));
                    grid.addView(createPairRow("TAC", String.valueOf(identity.getTac()), "S-RSRQ", ssRsrq + " dB"));
                    grid.addView(createPairRow("PCI", String.valueOf(identity.getPci()), "S-SNR", ssSinr + " dB"));
                    grid.addView(createPairRow("TA", ta + " | " + formatDistance(ta, isHighBand), "BW", "100 MHz"));

                    card.addView(grid);
                    cardsContainer.addView(card);
                }
            }
        } catch (SecurityException ignored) {}
    }

    private LinearLayout createTopRow(String leftText, String rightText) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        TextView left = new TextView(this);
        left.setText(leftText);
        left.setTextColor(Color.WHITE);
        left.setTextSize(19);
        left.setTypeface(null, android.graphics.Typeface.BOLD);
        left.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView right = new TextView(this);
        right.setText(rightText);
        right.setTextColor(Color.parseColor("#B0BEC5"));
        right.setTextSize(15);
        right.setGravity(Gravity.END);
        right.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

        row.addView(left);
        row.addView(right);
        return row;
    }

    private TextView createBadgeRow(String badgeText) {
        TextView badge = new TextView(this);
        badge.setText(badgeText);
        badge.setTextColor(Color.parseColor("#00FF66"));
        badge.setTextSize(14);
        badge.setTypeface(null, android.graphics.Typeface.BOLD);
        badge.setPadding(0, 2, 0, 6);
        return badge;
    }

    private LinearLayout createPairRow(String l1, String v1, String l2, String v2) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 3, 0, 3);

        // Left Column
        LinearLayout p1 = new LinearLayout(this);
        p1.setOrientation(LinearLayout.HORIZONTAL);
        p1.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView lbl1 = new TextView(this);
        lbl1.setText(l1 + "   ");
        lbl1.setTextColor(Color.parseColor("#9AA0A6"));
        lbl1.setTextSize(14);

        TextView val1 = new TextView(this);
        val1.setText(v1);
        val1.setTextColor(Color.WHITE);
        val1.setTextSize(14);
        val1.setTypeface(null, android.graphics.Typeface.BOLD);

        p1.addView(lbl1);
        p1.addView(val1);

        // Right Column
        LinearLayout p2 = new LinearLayout(this);
        p2.setOrientation(LinearLayout.HORIZONTAL);
        p2.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView lbl2 = new TextView(this);
        lbl2.setText(l2 + "   ");
        lbl2.setTextColor(Color.parseColor("#9AA0A6"));
        lbl2.setTextSize(14);

        TextView val2 = new TextView(this);
        val2.setText(v2);
        val2.setTextColor(Color.WHITE);
        val2.setTextSize(14);
        val2.setTypeface(null, android.graphics.Typeface.BOLD);

        p2.addView(lbl2);
        p2.addView(val2);

        row.addView(p1);
        row.addView(p2);
        return row;
    }
}
