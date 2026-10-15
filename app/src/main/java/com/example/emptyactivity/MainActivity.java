package com.example.emptyactivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.telephony.*;
import android.text.*;
import android.text.style.*;
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
    private final Typeface tfReg = Typeface.create("sans-serif", Typeface.NORMAL);
    private final Typeface tfMed = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private final Typeface tfBold = Typeface.create("sans-serif", Typeface.BOLD);
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

        int sbH = dp(38);
        int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resId > 0) sbH = getResources().getDimensionPixelSize(resId);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, sbH + dp(6), 0, dp(24));

        sv.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                int top = insets.getSystemWindowInsetTop();
                int bot = insets.getSystemWindowInsetBottom();
                root.setPadding(0, (top > 0 ? top : dp(38)) + dp(6), 0, (bot > 0 ? bot : dp(16)) + dp(12));
                return insets;
            }
        });

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

    private CharSequence fmt(String s) {
        if (s == null || s.isEmpty()) return "";
        SpannableString sp = new SpannableString(s);
        Matcher m = Pattern.compile("(dBm|dB|MHz|km|\\bm\\b)").matcher(s);
        while (m.find()) {
            sp.setSpan(new RelativeSizeSpan(0.78f), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        Matcher p = Pattern.compile("\\|").matcher(s);
        while (p.find()) {
            sp.setSpan(new ForegroundColorSpan(0xFF687078), p.start(), p.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return sp;
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

    private String bandCode(int ch, boolean nr) {
        if (nr) {
            if (ch >= 151600 && ch <= 160600) return "n28";
            if (ch >= 620000 && ch <= 653333) return "n78";
            if (ch > 653333 && ch <= 680000) return "n77";
            if (ch >= 499200 && ch <= 537999) return "n41";
            if (ch >= 460000 && ch <= 480000) return "n40";
            if (ch >= 422000 && ch <= 434000) return "n1";
            if (ch >= 361000 && ch <= 376000) return "n3";
            return "NR";
        } else {
            if (ch >= 9210 && ch <= 9659) return "B28";
            if (ch >= 0 && ch <= 599) return "B1";
            if (ch >= 1200 && ch <= 1949) return "B3";
            if (ch >= 2400 && ch <= 2649) return "B5";
            if (ch >= 3450 && ch <= 3799) return "B8";
            if (ch >= 38650 && ch <= 39649) return "B40";
            if (ch >= 39650 && ch <= 41589) return "B41";
            return "LTE";
        }
    }

    private String taText(int ta, boolean high5G) {
        if (ta < 0 || ta >= 1282) return "N/A";
        long m = Math.round(ta * (high5G ? 39.0625 : 78.125));
        return ta + " | " + (m >= 1000 ? String.format(Locale.US, "%.1f km", m / 1000.0) : m + " m");
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
        if (!nr && ch == 415) return "5 MHz";
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
                    int rssi = num(raw, "rssi=(-?\\d+)", -103);
                    int snr = num(raw, "rssnr=(-?\\d+)", sg.getRssnr());
                    int ta = num(raw, "ta=(-?\\d+)", sg.getTimingAdvance());

                    LinearLayout card = cardView(rsrp, op, mcc + " " + mnc, "⚓  4G | LTE " + fq + " | " + bw);
                    card.addView(gridRow("CI", String.valueOf(ci), "EARFCN", String.valueOf(id.getEarfcn())));
                    card.addView(gridRow("eNb", String.valueOf(ci >> 8), "RSSI", rssi + " dBm"));
                    card.addView(gridRow("CID", String.valueOf(ci & 0xFF), "RSRP", rsrp + " dBm"));
                    card.addView(gridRow("TAC", String.valueOf(id.getTac()), "RSRQ", rsrq + " dB"));
                    card.addView(gridRow("PCI", String.valueOf(id.getPci()), "SNR", (snr > -50 && snr < 60) ? snr + " dB" : "N/A"));
                    card.addView(gridRow("BW", bw, "TA", (ta >= 0 && ta < 1282) ? taText(ta, false) : "N/A"));
                    card.addView(calcBtn());
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

                    LinearLayout card = cardView(rsrp, op, mcc + " " + mnc, "⚓  5G | SA " + fq);
                    card.addView(gridRow("CI", String.valueOf(nci), "ARFCN", String.valueOf(arfcn)));
                    card.addView(gridRow("gNB", String.valueOf(nci >> 12), "S-RSRP", rsrp + " dBm"));
                    card.addView(gridRow("CID", String.valueOf(nci & 0xFFF), "S-RSRQ", rsrq + " dB"));
                    card.addView(gridRow("TAC", String.valueOf(id.getTac()), "S-SNR", sinr + " dB"));
                    card.addView(gridRow("PCI", String.valueOf(id.getPci()), "TA", taText(ta, high)));
                    card.addView(gridRow("BW", bw, "", ""));
                    card.addView(calcBtn());
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
                    root.addView(nbRow("J | " + bandCode(ch, true), id.getPci(), ch,
                        String.valueOf(sg.getSsRsrp()), String.valueOf(sg.getSsRsrq()),
                        (sinr >= -50 && sinr <= 50) ? String.valueOf(sinr) : "-", sg.getSsRsrp()));
                    root.addView(div(1, 0xFF181B1E));
                } else if (c instanceof CellInfoLte) {
                    CellIdentityLte id = ((CellInfoLte) c).getCellIdentity();
                    CellSignalStrengthLte sg = ((CellInfoLte) c).getCellSignalStrength();
                    int ch = id.getEarfcn();
                    if (id.getPci() < 0 || ch < 0) continue;
                    int rssi = num(raw, "rssi=(-?\\d+)", sg.getRsrp() + 12);
                    String pfx = (ch == 9560 || ch == 415) ? "B | " : "L | ";
                    root.addView(nbRow(pfx + bandCode(ch, false), id.getPci(), ch,
                        String.valueOf(rssi), String.valueOf(sg.getRsrp()),
                        String.valueOf(sg.getRsrq()), sg.getRsrp()));
                    root.addView(div(1, 0xFF181B1E));
                }
            }
        } catch (SecurityException ignored) {}
    }

    private LinearLayout cardView(int rsrp, String op, String mccmnc, String sub) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(16));

        int glowCore, glowMid, barCol;
        if (rsrp >= -85) {
            glowCore = 0x993B7A3E; glowMid = 0x38254D28; barCol = 0xCC55A659;
        } else if (rsrp >= -95) {
            glowCore = 0x99587324; glowMid = 0x38374717; barCol = 0xCC82A636;
        } else if (rsrp >= -105) {
            glowCore = 0x99706122; glowMid = 0x38453B15; barCol = 0xCCA89232;
        } else if (rsrp >= -115) {
            glowCore = 0x99635124; glowMid = 0x383B3016; barCol = 0xCC8F7534;
        } else {
            glowCore = 0x99663826; glowMid = 0x38402218; barCol = 0xCC995239;
        }

        GradientDrawable base = new GradientDrawable();
        base.setColor(0xFF101215);

        GradientDrawable radial = new GradientDrawable();
        radial.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        radial.setGradientCenter(0.32f, 0.12f);
        radial.setGradientRadius(dp(280));
        radial.setColors(new int[]{glowCore, glowMid, 0x00101215});

        card.setBackground(new LayerDrawable(new Drawable[]{base, radial}));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        left.addView(tv(op, 19, 0xFFFFFFFF, tfBold));

        View bar = new View(this);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(barCol);
        bg.setCornerRadius(dp(4));
        bar.setBackground(bg);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(58), dp(5));
        bp.setMargins(0, dp(2), 0, 0);
        bar.setLayoutParams(bp);
        left.addView(bar);

        top.addView(left);
        top.addView(tv(mccmnc, 15, 0xFFC4C7C5, tfMed));
        card.addView(top);

        TextView tvSub = tv(fmt(sub), 15, 0xFFE2E6EB, tfMed);
        tvSub.setPadding(0, dp(10), 0, dp(10));
        card.addView(tvSub);
        return card;
    }

    private LinearLayout gridRow(String l1, String v1, String l2, String v2) {
        LinearLayout r = new LinearLayout(this);
        r.setPadding(0, dp(3), 0, dp(3));
        r.addView(cell(l1, 0xFFC4C7C5, tfReg, 0.72f));
        r.addView(cell(fmt(v1), 0xFFFFFFFF, tfMed, 1.38f));
        r.addView(cell(l2, 0xFFC4C7C5, tfReg, 0.85f));
        r.addView(cell(fmt(v2), 0xFFFFFFFF, tfMed, 1.05f));
        return r;
    }

    private TextView cell(CharSequence t, int c, Typeface tf, float w) {
        TextView v = tv(t, 15, c, tf);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, -2, w));
        return v;
    }

    private TextView calcBtn() {
        TextView b = tv("✣  Calculate location", 15, 0xFF4CA6D8, tfMed);
        b.setGravity(Gravity.CENTER);
        b.setPadding(0, dp(11), 0, dp(11));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF192B36);
        bg.setCornerRadius(dp(10));
        b.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(12), 0, dp(2));
        b.setLayoutParams(lp);
        return b;
    }

    private TextView pill(String t) {
        TextView p = tv(fmt(t), 12, 0xFF38C976, tfMed);
        p.setGravity(Gravity.CENTER);
        p.setLayoutParams(new LinearLayout.LayoutParams(dp(68), dp(23)));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF10241B);
        bg.setCornerRadius(dp(6));
        bg.setStroke(dp(1), 0xFF268C53);
        p.setBackground(bg);
        return p;
    }

    private LinearLayout nbRow(String tag, int pci, int ch, String s1, String s2, String s3, int rsrp) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(16), dp(12), dp(14), dp(12));
        r.addView(pill(tag));

        TextView tvPci = tv(String.valueOf(pci), 16, 0xFFFFFFFF, tfMed);
        tvPci.setPadding(dp(12), 0, 0, 0);
        tvPci.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        r.addView(tvPci);

        r.addView(fixedTv(String.valueOf(ch), 14, 0xFFC4C7C5, tfReg, dp(62), Gravity.END));
        r.addView(fixedTv(s1, 16, 0xFFFFFFFF, tfMed, dp(42), Gravity.END));
        r.addView(fixedTv("|", 15, 0xFF687078, tfReg, dp(12), Gravity.CENTER));
        r.addView(fixedTv(s2, 16, 0xFFFFFFFF, tfMed, dp(34), Gravity.END));
        r.addView(fixedTv("|", 15, 0xFF687078, tfReg, dp(12), Gravity.CENTER));
        r.addView(fixedTv(s3, 16, 0xFFFFFFFF, tfMed, dp(30), Gravity.END));

        int dotCol = rsrp >= -95 ? 0xFF66BB6A : rsrp >= -110 ? 0xFFA8B545 : rsrp >= -120 ? 0xFFE67E45 : 0xFFEF5350;
        r.addView(fixedTv("●", 11, dotCol, tfReg, dp(18), Gravity.END));
        return r;
    }

    private TextView fixedTv(CharSequence t, int sp, int col, Typeface tf, int wPx, int grav) {
        TextView v = tv(t, sp, col, tf);
        v.setGravity(grav);
        v.setLayoutParams(new LinearLayout.LayoutParams(wPx, -2));
        return v;
    }

    private TextView tv(CharSequence t, int sp, int col, Typeface tf) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(sp);
        v.setTextColor(col);
        v.se
