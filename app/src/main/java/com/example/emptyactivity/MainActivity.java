package com.example.emptyactivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.telephony.*;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.util.*;
import java.util.regex.*;

public class MainActivity extends AppCompatActivity {
    private TelephonyManager tm;
    private SubscriptionManager sm;
    private LinearLayout root;
    private final Handler h = new Handler(Looper.getMainLooper());
    private final Runnable task = new Runnable() {
        @Override public void run() { update(); h.postDelayed(this, 1000); }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        getWindow().setStatusBarColor(0xFF0E1013);
        getWindow().setNavigationBarColor(0xFF0E1013);

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(0xFF0E1013);
        sv.setFillViewport(true);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        sv.addView(root);
        setContentView(sv);

        tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        sm = (SubscriptionManager) getSystemService(TELEPHONY_SUBSCRIPTION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != 0 ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != 0) {
            ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE
            }, 1);
        }
    }

    @Override protected void onResume() { super.onResume(); h.post(task); }
    @Override protected void onPause() { super.onPause(); h.removeCallbacks(task); }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private int num(String s, String rx, int def) {
        try {
            Matcher m = Pattern.compile(rx).matcher(s);
            if (m.find()) {
                int v = Integer.parseInt(m.group(1));
                if (v != Integer.MAX_VALUE && v != -2147483648) return v;
            }
        } catch (Exception ignored) {}
        return def;
    }

    private String opName(String mcc, String mnc, CharSequence alpha) {
        if ("404".equals(mcc) && "74".equals(mnc)) return "BSNL";
        if ("405".equals(mcc) && "840".equals(mnc)) return "JIO";
        String a = alpha != null ? alpha.toString().toUpperCase() : "";
        if (a.contains("JIO")) return "JIO";
        if (a.contains("BSNL") || a.contains("CELLONE")) return "BSNL";
        if (a.contains("VI") || a.contains("VODAFONE")) return "VI";
        if (a.contains("AIRTEL")) return "AIRTEL";
        return a.isEmpty() ? "SIM" : a;
    }

    private String freqName(int ch, boolean nr) {
        if (nr) {
            if (ch >= 151600 && ch <= 160600) return "700";
            if (ch >= 620000 && ch <= 653333) return "3500";
            if (ch > 653333 && ch <= 680000) return "3700";
            if (ch >= 499200 && ch <= 537999) return "2500";
            if (ch >= 460000 && ch <= 480000) return "2300";
            if (ch >= 422000 && ch <= 434000) return "2100";
            if (ch >= 361000 && ch <= 376000) return "1800";
        } else {
            if (ch >= 9210 && ch <= 9659) return "700";
            if (ch >= 0 && ch <= 599) return "2100";
            if (ch >= 1200 && ch <= 1949) return "1800";
            if (ch >= 2400 && ch <= 2649) return "850";
            if (ch >= 3450 && ch <= 3799) return "900";
            if (ch >= 38650 && ch <= 39649) return "2300";
            if (ch >= 39650 && ch <= 41589) return "2500";
        }
        return String.valueOf(ch);
    }

    private String taText(int ta, boolean high5G) {
        if (ta < 0 || ta >= 1282) return "N/A";
        long m = Math.round(ta * (high5G ? 39.0625 : 78.125));
        return ta + "  |  " + (m >= 1000 ? String.format(Locale.US, "%.2f km", m / 1000.0) : m + " m");
    }

    private String bwText(String mnc, boolean nr, int khz, int ch) {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == 0 && sm != null && mnc != null) {
                int target = Integer.parseInt(mnc);
                List<SubscriptionInfo> subs = sm.getActiveSubscriptionInfoList();
                if (subs != null) {
                    for (SubscriptionInfo s : subs) {
                        if (s.getMnc() == target) {
                            ServiceState ss = tm.createForSubscriptionId(s.getSubscriptionId()).getServiceState();
                            if (ss != null) {
                                Matcher m = Pattern.compile("mCellBandwidths=\\[([0-9,\\s]+)\\]").matcher(ss.toString());
                                if (m.find()) {
                                    String[] p = m.group(1).trim().split("\\s*,\\s*");
                                    StringBuilder sb = new StringBuilder();
                                    for (String x : p) {
                                        int v = Integer.parseInt(x) / 1000;
                                        if (v > 0) { if (sb.length() > 0) sb.append(" + "); sb.append(v); }
                                    }
                                    if (sb.length() > 0) return sb + " MHz";
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        if (khz > 0 && khz < 1000000) return (khz / 1000) + " MHz";
        return (nr && ch >= 620000) ? "100 MHz" : "10 MHz";
    }

    private void update() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != 0) return;
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                tm.requestCellInfoUpdate(getMainExecutor(), new TelephonyManager.CellInfoCallback() {
                    @Override public void onCellInfo(List<CellInfo> list) {}
                });
            } catch (Exception ignored) {}
        }
        root.removeAllViews();
        try {
            List<CellInfo> all = tm.getAllCellInfo();
            if (all == null) return;
            for (CellInfo c : all) {
                if (!c.isRegistered()) continue;
                String raw = c.toString();
                if (c instanceof CellInfoLte) {
                    CellIdentityLte id = ((CellInfoLte) c).getCellIdentity();
                    CellSignalStrengthLte sg = ((CellInfoLte) c).getCellSignalStrength();
                    int ci = id.getCi();
                    if (ci == Integer.MAX_VALUE || ci == 268435455 || ci < 0) continue;
                    String mcc = id.getMccString(), mnc = id.getMncString();
                    String op = opName(mcc, mnc, id.getOperatorAlphaLong());
                    String fq = freqName(id.getEarfcn(), false);
                    String bw = bwText(mnc, false, id.getBandwidth(), id.getEarfcn());
                    int rsrp = sg.getRsrp(), rsrq = sg.getRsrq();
                    int rssi = num(raw, "rssi=(-?\\d+)", -105);
                    int snr = num(raw, "rssnr=(-?\\d+)", sg.getRssnr());
                    int ta = num(raw, "ta=(-?\\d+)", sg.getTimingAdvance());

                    LinearLayout card = cardView(rsrp, op, mcc + " " + mnc, "⚓  4G  |  LTE " + fq + "  |  " + bw);
                    card.addView(gridRow("CI", String.valueOf(ci), "EARFCN", String.valueOf(id.getEarfcn())));
                    card.addView(gridRow("eNb:CID", (ci >> 8) + ":" + (ci & 0xFF), "RSSI", rssi + " dBm"));
                    card.addView(gridRow("TAC", String.valueOf(id.getTac()), "RSRP", rsrp + " dBm"));
                    card.addView(gridRow("PCI", String.valueOf(id.getPci()), "RSRQ", rsrq + " dB"));
                    card.addView(gridRow("BW", bw, "SNR", (snr > -50 && snr < 60) ? snr + " dB" : "N/A"));
                    if (ta >= 0 && ta < 1282) card.addView(gridRow("TA", taText(ta, false), "", ""));
                    card.addView(footerView(op.charAt(0) + " | " + fq));
                    root.addView(card);
                    root.addView(div(2, 0xFF22262A));
                } else if (c instanceof CellInfoNr) {
                    CellIdentityNr id = (CellIdentityNr) ((CellInfoNr) c).getCellIdentity();
                    CellSignalStrengthNr sg = (CellSignalStrengthNr) ((CellInfoNr) c).getCellSignalStrength();
                    long nci = id.getNci();
                    int arfcn = id.getNrarfcn();
                    String mcc = id.getMccString(), mnc = id.getMncString();
                    String op = opName(mcc, mnc, id.getOperatorAlphaLong());
                    String fq = freqName(arfcn, true);
                    String bw = bwText(mnc, true, -1, arfcn);
                    int rsrp = sg.getSsRsrp(), rsrq = sg.getSsRsrq(), sinr = sg.getSsSinr();
                    int ta = num(raw, "timingAdvance\\s*=\\s*(-?\\d+)", -1);
                    boolean high = arfcn >= 460000 && arfcn <= 680000;

                    LinearLayout card = cardView(rsrp, op, mcc + " " + mnc, "⚓  5G  |  SA " + fq);
                    card.addView(gridRow("CI", String.valueOf(nci), "ARFCN", String.valueOf(arfcn)));
                    card.addView(gridRow("gNB:CID", (nci >> 12) + ":" + (nci & 0xFFF), "S-RSRP", rsrp + " dBm"));
                    card.addView(gridRow("TAC", String.valueOf(id.getTac()), "S-RSRQ", rsrq + " dB"));
                    card.addView(gridRow("PCI", String.valueOf(id.getPci()), "S-SNR", sinr + " dB"));
                    card.addView(gridRow("BW", bw, "TA", taText(ta, high)));
                    root.addView(card);
                    root.addView(div(2, 0xFF22262A));
                }
            }
            for (CellInfo c : all) {
                if (c.isRegistered()) continue;
                String raw = c.toString();
                if (c instanceof CellInfoNr) {
                    CellIdentityNr id = (CellIdentityNr) ((CellInfoNr) c).getCellIdentity();
                    CellSignalStrengthNr sg = (CellSignalStrengthNr) ((CellInfoNr) c).getCellSignalStrength();
                    int ch = id.getNrarfcn(), sinr = sg.getSsSinr();
                    root.addView(nbRow("J | " + freqName(ch, true), id.getPci(), ch, sg.getSsRsrp(), sg.getSsRsrq(),
                        (sinr >= -50 && sinr <= 50) ? String.valueOf(sinr) : "-"));
                    root.addView(div(1, 0xFF181B1E));
                } else if (c instanceof CellInfoLte) {
                    CellIdentityLte id = ((CellInfoLte) c).getCellIdentity();
                    CellSignalStrengthLte sg = ((CellInfoLte) c).getCellSignalStrength();
                    int ch = id.getEarfcn(), snr = num(raw, "rssnr=(-?\\d+)", sg.getRssnr());
                    if (id.getPci() < 0 || ch < 0) continue;
                    String pfx = (ch == 9560 || ch == 415) ? "B | " : "L | ";
                    root.addView(nbRow(pfx + freqName(ch, false), id.getPci(), ch, sg.getRsrp(), sg.getRsrq(),
                        (snr > -50 && snr < 50 && snr != 0) ? String.valueOf(snr) : "-"));
                    root.addView(div(1, 0xFF181B1E));
                }
            }
        } catch (SecurityException ignored) {}
    }

    private LinearLayout cardView(int rsrp, String op, String mccmnc, String sub) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(16));
        int glow = rsrp >= -85 ? 0x552E6B3E : rsrp >= -95 ? 0x554A6321 : rsrp >= -105 ? 0x555A4D1E : rsrp >= -115 ? 0x554E3A1E : 0x55542424;
        card.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{glow, 0xFF16191D, 0xFF101215}));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        TextView tvOp = tv(op, 19, 0xFFFFFFFF, true);
        left.addView(tvOp);

        View bar = new View(this);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x88A67C2E);
        bg.setCornerRadius(dp(4));
        bar.setBackground(bg);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(52), dp(4));
        bp.setMargins(0, dp(2), 0, 0);
        bar.setLayoutParams(bp);
        left.addView(bar);

        top.addView(left);
        top.addView(tv(mccmnc, 15, 0xFFC4C7C5, false));
        card.addView(top);

        TextView tvSub = tv(sub, 15, 0xFFDCE0E5, false);
        tvSub.setPadding(0, dp(10), 0, dp(10));
        card.addView(tvSub);
        return card;
    }

    private LinearLayout gridRow(String l1, String v1, String l2, String v2) {
        LinearLayout r = new LinearLayout(this);
        r.setPadding(0, dp(3), 0, dp(3));
        r.addView(cell(l1, 0xFFC4C7C5, 0.85f));
        r.addView(cell(v1, 0xFFFFFFFF, 1.35f));
        r.addView(cell(l2, 0xFFC4C7C5, 0.85f));
        r.addView(cell(v2, 0xFFFFFFFF, 1.15f));
        return r;
    }

    private TextView cell(String t, int c, float w) {
        TextView tv = tv(t, 15, c, false);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, -2, w));
        return tv;
    }

    private LinearLayout footerView(String tag) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(0, dp(10), 0, 0);
        r.addView(pill(tag));
        r.addView(tv("  Tap to add location", 15, 0xFF2EA866, false));
        return r;
    }

    private TextView pill(String t) {
        TextView p = tv(t, 12, 0xFF4CD987, true);
        p.setPadding(dp(8), dp(2), dp(8), dp(2));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF122B21);
        bg.setCornerRadius(dp(6));
        bg.setStroke(dp(1), 0xFF2EA866);
        p.setBackground(bg);
        return p;
    }

    private LinearLayout nbRow(String tag, int pci, int ch, int rsrp, int rsrq, String snr) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(16), dp(12), dp(16), dp(12));
        r.addView(pill(tag));
        TextView tvPci = tv("   " + pci, 16, 0xFFFFFFFF, false);
        tvPci.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        r.addView(tvPci);
        r.addView(tv(ch + "   ", 14, 0xFF9AA0A6, false));
        r.addView(tv(rsrp + " | " + rsrq + " | " + snr + "  ", 16, 0xFFFFFFFF, false));
        r.addView(tv("●", 12, rsrp >= -90 ? 0xFF4CAF50 : rsrp >= -105 ? 0xFFFFA726 : 0xFFEF5350, false));
        return r;
    }

    private TextView tv(String t, int sp, int col, boolean bold) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(sp);
        v.setTextColor(col);
        if (bold) v.setTypeface(null, Typeface.BOLD);
        return v;
    }

    private View div(int hDp, int col) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(hDp)));
        v.setBackgroundColor(col);
        return v;
    }
}
// END OF FILE
