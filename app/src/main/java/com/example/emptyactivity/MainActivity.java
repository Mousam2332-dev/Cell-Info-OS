package com.example.emptyactivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.*;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private TelephonyManager telephonyManager;
    private SubscriptionManager subscriptionManager;
    private LinearLayout mainContainer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        getWindow().setStatusBarColor(Color.parseColor("#0E1013"));
        getWindow().setNavigationBarColor(Color.parseColor("#0E1013"));
        setContentView(R.layout.activity_main);

        mainContainer = findViewById(R.id.cards_container);
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        subscriptionManager = (SubscriptionManager) getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);

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
                handler.postDelayed(this, 1000);
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

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private int extractInt(String src, String regex, int def) {
        try {
            Matcher m = Pattern.compile(regex).matcher(src);
            if (m.find()) {
                int v = Integer.parseInt(m.group(1));
                if (v != Integer.MAX_VALUE && v != 2147483647 && v != -2147483648) return v;
            }
        } catch (Exception ignored) {}
        return def;
    }

    private String getOperatorName(String mcc, String mnc, String rawAlpha) {
        if ("404".equals(mcc) && "74".equals(mnc)) return "BSNL";
        if ("405".equals(mcc) && "840".equals(mnc)) return "JIO";
        if (rawAlpha != null && !rawAlpha.isEmpty() && !"null".equals(rawAlpha)) {
            String up = rawAlpha.toUpperCase();
            if (up.contains("JIO")) return "JIO";
            if (up.contains("BSNL") || up.contains("CELLONE")) return "BSNL";
            if (up.contains("VI") || up.contains("VODAFONE")) return "VI";
            if (up.contains("AIRTEL")) return "AIRTEL";
            return up;
        }
        return "SIM";
    }

    private String getLteFreq(int earfcn, int band) {
        if (earfcn >= 9210 && earfcn <= 9659) return "700";
        if (earfcn >= 0 && earfcn <= 599) return "2100";
        if (earfcn >= 1200 && earfcn <= 1949) return "1800";
        if (earfcn >= 2400 && earfcn <= 2649) return "850";
        if (earfcn >= 3450 && earfcn <= 3799) return "900";
        if (earfcn >= 38650 && earfcn <= 39649) return "2300";
        if (earfcn >= 39650 && earfcn <= 41589) return "2500";
        return band > 0 ? "B" + band : String.valueOf(earfcn);
    }

    private String getNrFreq(int arfcn, int band) {
        if (arfcn >= 151600 && arfcn <= 160600) return "700";
        if (arfcn >= 620000 && arfcn <= 653333) return "3500";
        if (arfcn > 653333 && arfcn <= 680000) return "3700";
        if (arfcn >= 499200 && arfcn <= 537999) return "2500";
        if (arfcn >= 460000 && arfcn <= 480000) return "2300";
        if (arfcn >= 422000 && arfcn <= 434000) return "2100";
        if (arfcn >= 361000 && arfcn <= 376000) return "1800";
        return band > 0 ? "n" + band : String.valueOf(arfcn);
    }

    private boolean is5gHighBand(int arfcn, int band) {
        return band == 78 || band == 77 || band == 41 || band == 40 || (arfcn >= 460000 && arfcn <= 680000);
    }

    private String formatTa(int ta, boolean highBand5G) {
        if (ta < 0 || ta >= 1282) return "N/A";
        double step = highBand5G ? 39.0625 : 78.125;
        long m = Math.round(ta * step);
        return m >= 1000 ? String.format("%.2f km", m / 1000.0) : (m + " m");
    }

    private String getBandwidth(String mcc, String mnc, boolean is5G, int fallbackKhz) {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED && subscriptionManager != null) {
                List<SubscriptionInfo> subs = subscriptionManager.getActiveSubscriptionInfoList();
                if (subs != null && mnc != null) {
                    int targetMnc = Integer.parseInt(mnc);
                    for (SubscriptionInfo sub : subs) {
                        if (sub.getMnc() == targetMnc) {
                            ServiceState ss = telephonyManager.createForSubscriptionId(sub.getSubscriptionId()).getServiceState();
                            if (ss != null) {
                                Matcher m = Pattern.compile("mCellBandwidths=\\[([0-9,\\s]+)\\]").matcher(ss.toString());
                                if (m.find()) {
                                    String[] parts = m.group(1).trim().split("\\s*,\\s*");
                                    StringBuilder bw = new StringBuilder();
                                    for (String p : parts) {
                                        if (!p.isEmpty()) {
                                            int mhz = Integer.parseInt(p) / 1000;
                                            if (mhz > 0) {
                                                if (bw.length() > 0) bw.append(" + ");
                                                bw.append(mhz);
                                            }
                                        }
                                    }
                                    if (bw.length() > 0) return bw.toString() + " MHz";
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        if (fallbackKhz > 0 && fallbackKhz < 1000000) return (fallbackKhz / 1000) + " MHz";
        return is5G ? "100 MHz" : "10 MHz";
    }

    private GradientDrawable createCardBg(int rsrp) {
        int glow;
        if (rsrp >= -85) glow = Color.parseColor("#552E6B3E");
        else if (rsrp >= -95) glow = Color.parseColor("#554A6321");
        else if (rsrp >= -105) glow = Color.parseColor("#555A4D1E");
        else if (rsrp >= -115) glow = Color.parseColor("#554E3A1E");
        else glow = Color.parseColor("#55542424");

        return new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{glow, Color.parseColor("#16191D"), Color.parseColor("#101215")}
        );
    }

    private void updateCellMetrics() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            try {
                telephonyManager.requestCellInfoUpdate(getMainExecutor(), new TelephonyManager.CellInfoCallback() {
                    @Override
                    public void onCellInfo(List<CellInfo> cellInfo) {}
                });
            } catch (Exception ignored) {}
        }

        mainContainer.removeAllViews();

        try {
            List<CellInfo> allCells = telephonyManager.getAllCellInfo();
            if (allCells == null || allCells.isEmpty()) return;

            List<CellInfo> serving = new ArrayList<>();
            List<CellInfo> neighbors = new ArrayList<>();
            for (CellInfo c : allCells) {
                if (c.isRegistered()) serving.add(c);
                else neighbors.add(c);
            }

            for (CellInfo cell : serving) {
                String raw = cell.toString();
                if (cell instanceof CellInfoLte) {
                    CellInfoLte lte = (CellInfoLte) cell;
                    CellIdentityLte id = lte.getCellIdentity();
                    CellSignalStrengthLte sig = lte.getCellSignalStrength();

                    int ci = id.getCi();
                    if (ci == 268435455 || ci == -1 || ci == Integer.MAX_VALUE) continue;

                    int enb = ci >> 8;
                    int cid = ci & 0xFF;
                    int earfcn = id.getEarfcn();
                    int band = (id.getBands() != null && id.getBands().length > 0) ? id.getBands()[0] : extractInt(raw, "mBands=\\[(\\d+)", 0);
                    String mcc = id.getMccString();
                    String mnc = id.getMncString();
                    String op = getOperatorName(mcc, mnc, id.getOperatorAlphaLong() != null ? id.getOperatorAlphaLong().toString() : "");
                    String freq = getLteFreq(earfcn, band);
                    String bw = getBandwidth(mcc, mnc, false, id.getBandwidth());

                    int rsrp = sig.getRsrp();
                    int rsrq = sig.getRsrq();
                    int rssi = extractInt(raw, "rssi=(-?\\d+)", -105);
                    int snr = extractInt(raw, "rssnr=(-?\\d+)", sig.getRssnr());
                    int ta = extractInt(raw, "ta=(-?\\d+)", sig.getTimingAdvance());

                    LinearLayout card = new LinearLayout(this);
                    card.setOrientation(LinearLayout.VERTICAL);
                    card.setPadding(dp(18), dp(18), dp(18), dp(16));
                    card.setBackground(createCardBg(rsrp));

                    card.addView(createHeader(op, (mcc != null ? mcc : "") + " " + (mnc != null ? mnc : "")));
                    card.addView(createSubtitle("⚓  4G  |  LTE " + freq + "  |  " + bw));
                    card.addView(createRow("CI", String.valueOf(ci), "EARFCN", String.valueOf(earfcn)));
                    card.addView(createRow("eNb:CID", enb + ":" + cid, "RSSI", rssi + " dBm"));
                    card.addView(createRow("TAC", String.valueOf(id.getTac()), "RSRP", rsrp + " dBm"));
                    card.addView(createRow("PCI", String.valueOf(id.getPci()), "RSRQ", rsrq + " dB"));
                    String snrStr = (snr > -50 && snr < 60) ? snr + " dB" : "N/A";
                    if (ta >= 0 && ta < 1282) {
                        card.addView(createRow("BW", bw, "SNR", snrStr));
                        card.addView(createRow("TA", ta + " | " + formatTa(ta, false), "", ""));
                    } else {
                        card.addView(createRow("BW", bw, "SNR", snrStr));
                    }
                    card.addView(createFooter(op.substring(0, 1) + " | " + freq));
                    mainContainer.addView(card);
                    mainContainer.addView(createDivider(2, "#22262A"));

                } else if (cell instanceof CellInfoNr) {
                    CellInfoNr nr = (CellInfoNr) cell;
                    CellIdentityNr id = (CellIdentityNr) nr.getCellIdentity();
                    CellSignalStrengthNr sig = (CellSignalStrengthNr) nr.getCellSignalStrength();

                    long nci = id.getNci();
                    long gnb = nci >> 12;
                    long cid = nci & 0xFFF;
                    int arfcn = id.getNrarfcn();
                    int band = (id.getBands() != null && id.getBands().length > 0) ? id.getBands()[0] : extractInt(raw, "mBands\\s*=\\s*\\[(\\d+)", 0);
                    String mcc = id.getMccString();
                    String mnc = id.getMncString();
                    String op = getOperatorName(mcc, mnc, id.getOperatorAlphaLong() != null ? id.getOperatorAlphaLong().toString() : "");
                    String freq = getNrFreq(arfcn, band);
                    String bw = getBandwidth(mcc, mnc, true, -1);

                    int rsrp = sig.getSsRsrp();
                    int rsrq = sig.getSsRsrq();
                    int sinr = sig.getSsSinr();
                    int ta = extractInt(raw, "timingAdvance\\s*=\\s*(-?\\d+)", -1);
                    boolean high = is5gHighBand(arfcn, band);

                    LinearLayout card = new LinearLayout(this);
                    card.setOrientation(LinearLayout.VERTICAL);
                    card.setPadding(dp(18), dp(18), dp(18), dp(16));
                    card.setBackground(createCardBg(rsrp));

                    card.addView(createHeader(op, (mcc != null ? mcc : "") + " " + (mnc != null ? mnc : "")));
                    card.addView(createSubtitle("⚓  5G  |  SA " + freq));
                    card.addView(createRow("CI", String.valueOf(nci), "ARFCN", String.valueOf(arfcn)));
                    card.addView(createRow("gNB:CID", gnb + ":" + cid, "S-RSRP", rsrp + " dBm"));
                    card.addView(createRow("TAC", String.valueOf(id.getTac()), "S-RSRQ", rsrq + " dB"));
                    card.addView(createRow("PCI", String.valueOf(id.getPci()), "S-SNR", sinr + " dB"));
                    card.addView(createRow("BW", bw, "TA", (ta >= 0 && ta < 1282) ? (ta + " | " + formatTa(ta, high)) : "N/A"));

                    mainContainer.addView(card);
                    mainContainer.addView(createDivider(2, "#22262A"));
                }
            }

            for (CellInfo cell : neighbors) {
                String raw = cell.toString();
                if (cell instanceof CellInfoNr) {
                    CellInfoNr nr = (CellInfoNr) cell;
                    CellIdentityNr id = (CellIdentityNr) nr.getCellIdentity();
                    CellSignalStrengthNr sig = (CellSignalStrengthNr) nr.getCellSignalStrength();
                    int arfcn = id.getNrarfcn();
                    String freq = getNrFreq(arfcn, 0);
                    int rsrp = sig.getSsRsrp();
                    int rsrq = sig.getSsRsrq();
                    int sinr = sig.getSsSinr();
                    String snrStr = (sinr >= -50 && sinr <= 50) ? String.valueOf(sinr) : "-";
                    mainContainer.addView(createNeighbor("J | " + freq, String.valueOf(id.getPci()), String.valueOf(arfcn), rsrp, rsrq, snrStr));
                    mainContainer.addView(createDivider(1, "#181B1E"));
                } else if (cell instanceof CellInfoLte) {
                    CellInfoLte lte = (CellInfoLte) cell;
                    CellIdentityLte id = lte.getCellIdentity();
                    CellSignalStrengthLte sig = lte.getCellSignalStrength();
                    int earfcn = id.getEarfcn();
                    if (id.getPci() < 0 || earfcn < 0) continue;
                    String freq = getLteFreq(earfcn, 0);
                    String tag = (earfcn == 9560 || earfcn == 415) ? "B | " + freq : "L | " + freq;
                    int rsrp = sig.getRsrp();
                    int rsrq = sig.getRsrq();
                    int snr = extractInt(raw, "rssnr=(-?\\d+)", sig.getRssnr());
                    String snrStr = (snr > -50 && snr < 50 && snr != 0) ? String.valueOf(snr) : "-";
                    mainContainer.addView(createNeighbor(tag, String.valueOf(id.getPci()), String.valueOf(earfcn), rsrp, rsrq, snrStr));
                    mainContainer.addView(createDivider(1, "#181B1E"));
                }
            }
        } catch (SecurityException ignored) {}
    }

    private LinearLayout createHeader(String op, String mccmnc) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvOp = new TextView(this);
        tvOp.setText(op);
        tvOp.setTextColor(Color.WHITE);
        tvOp.setTextSize(19);
        tvOp.setTypeface(null, Typeface.BOLD);
        left.addView(tvOp);

        View bar = new View(this);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#88A67C2E"));
        bg.setCornerRadius(dp(4));
        bar.setBackground(bg);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(52), dp(4));
        bp.setMargins(0, dp(2), 0, 0);
        bar.setLayoutParams(bp);
        left.addView(bar);

        TextView tvRight = new TextView(this);
        tvRight.setText(mccmnc);
        tvRight.setTextColor(Color.parseColor("#C4C7C5"));
        tvRight.setTextSize(15);

        row.addView(left);
        row.addView(tvRight);
        return row;
    }

    private TextView createSubtitle(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#DCE0E5"));
        tv.setTextSize(15);
        tv.setPadding(0, dp(10), 0, dp(10));
        return tv;
    }

    private LinearLayout createRow(String l1, String v1, String l2, String v2) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(3), 0, dp(3));
        row.addView(createCell(l1, "#C4C7C5", 0.85f));
        row.addView(createCell(v1, "#FFFFFF", 1.35f));
        row.addView(createCell(l2, "#C4C7C5", 0.85f));
        row.addView(createCell(v2, "#FFFFFF", 1.15f));
        return row;
    }

    private TextView createCell(String text, String hex, float weight) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor(hex));
        tv.setTextSize(15);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight));
        return tv;
    }

    private LinearLayout createFooter(String pillText) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, 0);
        row.addView(createPill(pillText));

        TextView tv = new TextView(this);
        tv.setText("  Tap to add location");
        tv.setTextColor(Color.parseColor("#2EA866"));
        tv.setTextSize(15);
        row.addView(tv);
        return row;
    }

    private TextView createPill(String text) {
        TextView pill = new TextView(this);
        pill.setText(text);
        pill.setTextColor(Color.parseColor("#4CD987"));
        pill.setTextSize(12);
        pill.setTypeface(null, Typeface.BOLD);
        pill.setPadding(dp(8), dp(2), dp(8), dp(2));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#122B21"));
        bg.setCornerRadius(dp(6));
        bg.setStroke(dp(1), Color.parseColor("#2EA866"));
        pill.setBackground(bg);
        return pill;
    }

    private LinearLayout createNeighbor(String tag, String pci, String ar
