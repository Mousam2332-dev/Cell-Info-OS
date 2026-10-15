package com.example.emptyactivity;

import android.Manifest;
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
    private final Typeface tfR = Typeface.create("sans-serif", 0);
    private final Typeface tfM = Typeface.create("sans-serif-medium", 0);
    private final Typeface tfB = Typeface.create("sans-serif", 1);
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

        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        int topPad = (id > 0 ? getResources().getDimensionPixelSize(id) : dp(38)) + dp(6);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, topPad, 0, dp(24));

        sv.setOnApplyWindowInsetsListener((v, in) -> {
            int t = in.getSystemWindowInsetTop(), bt = in.getSystemWindowInsetBottom();
            root.setPadding(0, (t > 0 ? t : dp(38)) + dp(6), 0, (bt > 0 ? bt : dp(16)) + dp(12));
            return in;
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
        while (m.find()) sp.setSpan(new RelativeSizeSpan(0.78f), m.start(), m.end(), 33);
        Matcher p = Pattern.compile("\\|").matcher(s);
        while (p.find()) sp.setSpan(new ForegroundColorSpan(0xFF687078), p.start(), p.end(), 33);
        return sp;
    }

    private String opName(String mcc, String mnc, CharSequence a) {
        if ("404".equals(mcc) && "74".equals(mnc)) return "BSNL";
        if ("405".equals(mcc) && "840".equals(mnc)) return "JIO";
        String s = a != null ? a.toString().toUpperCase() : "";
        return s.contains("JIO") ? "JIO" : (s.contains("BSNL") || s.contains("CELLONE")) ? "BSNL" : s.isEmpty() ? "SIM" : s;
    }

    private String freq(int ch, boolean nr, boolean code) {
        if (nr) {
            if (ch >= 151600 && ch <= 160600) return code ? "n28" : "700";
            if (ch >= 620000 && ch <= 653333) return code ? "n78" : "3500";
            if (ch > 653333 && ch <= 680000) return code ? "n77" : "3700";
            if (ch >= 499200 && ch <= 537999) return code ? "n41" : "2500";
            if (ch >= 460000 && ch <= 480000) return code ? "n40" : "2300";
            if (ch >= 422000 && ch <= 434000) return code ? "n1" : "2100";
            if (ch >= 361000 && ch <= 376000) return code ? "n3" : "1800";
            return code ? "NR" : String.valueOf(ch);
        }
        if (ch >= 9210 && ch <= 9659) return code ? "B28" : "700";
        if (ch >= 0 && ch <= 599) return code ? "B1" : "2100";
        if (ch >= 1200 && ch <= 1949) return code ? "B3" : "1800";
        if (ch >= 2400 && ch <= 2649) return code ? "B5" : "850";
        if (ch >= 3450 && ch <= 3799) return code ? "B8" : "900";
        if (ch >= 38650 && ch <= 39649) return code ? "B40" : "2300";
        if (ch >= 39650 && ch <= 41589) return code ? "B41" : "2500";
        return code ? "LTE" : String.valueOf(ch);
    }

    private String taText(int ta, boolean hi) {
        if (ta < 0 || ta >= 1282) return "N/A";
        long m = Math.round(ta * (hi ? 39.0625 : 78.125));
        return ta + " | " + (m >= 1000 ? String.format(Locale.US, "%.1f km", m / 1000.0) : m + " m");
    }

    private String bwText(String mnc, boolean nr, int khz, int ch) {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == 0 && sm != null && mnc != null) {
                int t = Integer.parseInt(mnc);
                List<SubscriptionInfo> subs = sm.getActiveSubscriptionInfoList();
                if (subs != null) for (SubscriptionInfo s : subs) if (s.getMnc() == t) {
                    ServiceState ss = tm.createForSubscriptionId(s.getSubscriptionId()).getServiceState();
                    if (ss != null) {
                        Matcher m = Pattern.compile("mCellBandwidths=\\[([0-9,\\s]+)\\]").matcher(ss.toString());
                        if (m.find()) {
                            StringBuilder sb = new StringBuilder();
                            for (String x : m.group(1).trim().split("\\s*,\\s*")) {
                                int v = Integer.parseInt(x) / 1000;
                                if (v > 0) { if (sb.length() > 0) sb.append(" + "); sb.append(v); }
                            }
                            if (sb.length() > 0) return sb + " MHz";
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        if (khz > 0 && khz < 1000000) return (khz / 1000) + " MHz";
        return (!nr && ch == 415) ? "5 MHz" : (nr && ch >= 620000) ? "100 MHz" : "10 MHz";
    }

    private void update() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != 0) return;
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                tm.requestCellInfoUpdate(getMainExecutor(), new TelephonyManager.CellInfoCallback() {
                    @Override public void onCellInfo(List<CellInfo> l) {}
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
                    String mcc = id.getMccString(), mnc = id.getMncString(), op = opName(mcc, mnc, id.getOperatorAlphaLong());
                    String fq = freq(id.getEarfcn(), false, false), bw = bwText(mnc, false, id.getBandwidth(), id.getEarfcn());
                    int rsrp = sg.getRsrp(), rsrq = sg.getRsrq(), rssi = num(raw, "rssi=(-?\\d+)", -103);
                    int snr = num(raw, "rssnr=(-?\\d+)", sg.getRssnr()), ta = num(raw, "ta=(-?\\d+)", sg.getTimingAdvance());

                    LinearLayout card = cardView(rsrp, op, mcc + " " + mnc, "⚓  4G | LTE " + fq + " | " + bw);
                    card.addView(gRow("CI", String.valueOf(ci), "EARFCN", String.valueOf(id.getEarfcn())));
                    card.addView(gRow("eNb", String.valueOf(ci >> 8), "RSSI", rssi + " dBm"));
                    card.addView(gRow("CID", String.valueOf(ci & 0xFF), "RSRP", rsrp + " dBm"));
                    card.addView(gRow("TAC", String.valueOf(id.getTac()), "RSRQ", rsrq + " dB"));
                    card.addView(gRow("PCI", String.valueOf(id.getPci()), "SNR", (snr > -50 && snr < 60) ? snr + " dB" : "N/A"));
                    card.addView(gRow("BW", bw, "TA", taText(ta, false)));
                    card.addView(calcBtn());
                    root.addView(card);
                    root.addView(div(2, 0xFF22262A));
                } else if (c instanceof CellInfoNr) {
                    CellIdentityNr id = (CellIdentityNr) ((CellInfoNr) c).getCellIdentity();
                    CellSignalStrengthNr sg = (CellSignalStrengthNr) ((CellInfoNr) c).getCellSignalStrength();
                    long nci = id.getNci();
                    int ch = id.getNrarfcn(), rsrp = sg.getSsRsrp(), rsrq = sg.getSsRsrq(), sinr = sg.getSsSinr();
                    String mcc = id.getMccString(), mnc = id.getMncString(), op = opName(mcc, mnc, id.getOperatorAlphaLong());
                    String fq = freq(ch, true, false), bw = bwText(mnc, true, -1, ch);
                    int ta = num(raw, "timingAdvance\\s*=\\s*(-?\\d+)", -1);

                    LinearLayout card = cardView(rsrp, op, mcc + " " + mnc, "⚓  5G | SA " + fq);
                    card.addView(gRow("CI", String.valueOf(nci), "ARFCN", String.valueOf(ch)));
                    card.addView(gRow("gNB", String.valueOf(nci >> 12), "S-RSRP", rsrp + " dBm"));
                    card.addView(gRow("CID", String.valueOf(nci & 0xFFF), "S-RSRQ", rsrq + " dB"));
                    card.addView(gRow("TAC", String.valueOf(id.getTac()), "S-SNR", sinr + " dB"));
                    card.addView(gRow("PCI", String.valueOf(id.getPci()), "TA", taText(ta, ch >= 460000 && ch <= 680000)));
                    card.addView(gRow("BW", bw, "", ""));
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
                    root.addView(nbRow("J | " + freq(ch, true, true), id.getPci(), ch,
                        String.valueOf(sg.getSsRsrp()), String.valueOf(sg.getSsRsrq()),
                        (sinr >= -50 && sinr <= 50) ? String.valueOf(sinr) : "-", sg.getSsRsrp()));
                    root.addView(div(1, 0xFF181B1E));
                } else if (c instanceof CellInfoLte) {
                    CellIdentityLte id = ((CellInfoLte) c).getCellIdentity();
                    CellSignalStrengthLte sg = ((CellInfoLte) c).getCellSignalStrength();
                    int ch = id.getEarfcn();
                    if (id.getPci() < 0 || ch < 0) continue;
                    int rssi = num(raw, "rssi=(-?\\d+)", sg.getRsrp() + 12);
                    root.addView(nbRow(((ch == 9560 || ch == 415) ? "B | " : "L | ") + freq(ch, false, true),
                        id.getPci(), ch, String.valueOf(rssi), String.valueOf(sg.getRsrp()),
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
        int g1 = rsrp >= -85 ? 0x993B7A3E : rsrp >= -95 ? 0x99587324 : rsrp >= -105 ? 0x99706122 : rsrp >= -115 ? 0x99635124 : 0x99663826;
        int g2 = rsrp >= -85 ? 0x38254D28 : rsrp >= -95 ? 0x38374717 : rsrp >= -105 ? 0x38453B15 : rsrp >= -115 ? 0x383B3016 : 0x38402218;
        int bc = rsrp >= -85 ? 0xCC55A659 : rsrp >= -95 ? 0xCC82A636 : rsrp >= -105 ? 0xCCA89232 : rsrp >= -115 ? 0xCC8F7534 : 0xCC995239;

        GradientDrawable base = new GradientDrawable();
        base.setColor(0xFF101215);
        GradientDrawable rad = new GradientDrawable();
        rad.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        rad.setGradientCenter(0.32f, 0.12f);
        rad.setGradientRadius(dp(280));
        rad.setColors(new int[]{g1, g2, 0x00101215});
        card.setBackground(new LayerDrawable(new Drawable[]{base, rad}));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        left.addView(tv(op, 19, 0xFFFFFFFF, tfB));

        View bar = new View(this);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bc);
        bg.setCornerRadius(dp(4));
        bar.setBackground(bg);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(58), dp(5));
        bp.setMargins(0, dp(2), 0, 0);
        bar.setLayoutParams(bp);
        left.addView(bar);

        top.addView(left);
        top.addView(tv(mccmnc, 15, 0xFFC4C7C5, tfM));
        card.addView(top);

        TextView tvSub = tv(fmt(sub), 15, 0xFFE2E6EB, tfM);
        tvSub.setPadding(0, dp(10), 0, dp(10));
        card.addView(tvSub);
        return card;
    }

    private LinearLayout gRow(String l1, String v1, String l2, String v2) {
        LinearLayout r = new LinearLayout(this);
        r.setPadding(0, dp(3), 0, dp(3));
        r.addView(cell(l1, 0xFFC4C7C5, tfR, 0.72f));
        r.addView(cell(fmt(v1), 0xFFFFFFFF, tfM, 1.38f));
        r.addView(cell(l2, 0xFFC4C7C5, tfR, 0.85f));
        r.addView(cell(fmt(v2), 0xFFFFFFFF, tfM, 1.05f));
        return r;
    }

    private TextView cell(CharSequence t, int c, Typeface tf, float w) {
        TextView v = tv(t, 15, c, tf);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, -2, w));
        return v;
    }

    private TextView calcBtn() {
        TextView b = tv("✣  Calculate location", 15, 0xFF4CA6D8, tfM);
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

    private LinearLayout nbRow(String tag, int pci, int ch, String s1, String s2, String s3, int rsrp) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(16), dp(12), dp(14), dp(12));

        TextView p = tv(fmt(tag), 12, 0xFF38C976, tfM);
        p.setGravity(Gravity.CENTER);
        p.setLayoutParams(new LinearLayout.LayoutParams(dp(68), dp(23)));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF10241B);
        bg.setCornerRadius(dp(6));
        bg.setStroke(dp(1), 0xFF268C53);
        p.setBackground(bg);
        r.addView(p);

        TextView tvPci = tv(String.valueOf(pci), 16, 0xFFFFFFFF, tfM);
        tvPci.setPadding(dp(12), 0, 0, 0);
        tvPci.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        r.addView(tvPci);

        r.addView(fTv(String.valueOf(ch), 14, 0xFFC4C7C5, tfR, dp(62), Gravity.END));
        r.addView(fTv(s1, 16, 0xFFFFFFFF, tfM, dp(42), Gravity.END));
        r.addView(fTv("|", 15, 0xFF687078, tfR, dp(12), Gravity.CENTER));
        r.addView(fTv(s2, 16, 0xFFFFFFFF, tfM, dp(34), Gravity.END));
        r.addView(fTv("|", 15, 0xFF687078, tfR, dp(12), Gravity.CENTER));
        r.addView(fTv(s3, 16, 0xFFFFFFFF, tfM, dp(30), Gravity.END));

        int dc = rsrp >= -95 ? 0xFF66BB6A : rsrp >= -110 ? 0xFFA8B545 : rsrp >= -120 ? 0xFFE67E45 : 0xFFEF5350;
        r.addView(fTv("●", 11, dc, tfR, dp(18), Gravity.END));
        return r;
    }

    private TextView fTv(CharSequence t, int sp, int col, Typeface tf, int w, int g) {
        TextView v = tv(t, sp, col, tf);
        v.setGravity(g);
        v.setLayoutParams(new LinearLayout.LayoutParams(w, -2));
        return v;
    }

    private TextView tv(CharSequence t, int sp, int col, Typeface tf) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(sp);
        v.setTextColor(col);
        v.setTypeface(tf);
        v.setFontFeatureSettings("tnum");
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
            
