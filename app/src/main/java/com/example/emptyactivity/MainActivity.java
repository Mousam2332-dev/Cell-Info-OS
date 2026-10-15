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
import android.widget.ScrollView;
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
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        getWindow().setStatusBarColor(Color.parseColor("#0E1013"));
        getWindow().setNavigationBarColor(Color.parseColor("#0E1013"));

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.parseColor("#0E1013"));
        scrollView.setFillViewport(true);

        mainContainer = new LinearLayout(this);
        mainContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(mainContainer);
        setContentView(scrollView);

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

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
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
        if (mcc != null && (mcc.equals("404") || mcc.equals("405"))) {
            if ("10".equals(mnc) || "34".equals(mnc) || "51".equals(mnc) || "53".equals(mnc)) return "BSNL";
            if (mnc != null && mnc.startsWith("8")) return "JIO";
        }
        if (rawAlpha != null && !rawAlpha.isEmpty() && !rawAlpha.equals("null")) {
            if (rawAlpha.toUpperCase().contains("JIO")) return "JIO";
            if (rawAlpha.toUpperCase().contains("BSNL") || rawAlpha.toUpperCase().contains("CELLONE")) return "BSNL";
            if (rawAlpha.toUpperCase().contains("VI") || rawAlpha.toUpperCase().contains("VODAFONE")) return "VI INDIA";
            if (rawAlpha.toUpperCase().contains("AIRTEL")) return "AIRTEL";
            return rawAlpha.toUpperCase();
        }
        return "UNKNOWN";
    }

    private String getLteFreqName(int earfcn, int band) {
        if (band == 28 || (earfcn >= 9210 && earfcn <= 9659)) return "700";
        if (band == 1 || (earfcn >= 0 && earfcn <= 599)) return "2100";
        if (band == 3 || (earfcn >= 1200 && earfcn <= 1949)) return "1800";
        if (band == 5 || (earfcn >= 2400 && earfcn <= 2649)) return "850";
        if (band == 8 || (earfcn >= 3450 && earfcn <= 3799)) return "900";
        if (band == 40 || (earfcn >= 38650 && earfcn <= 39649)) return "2300";
        if (band == 41 || (earfcn >= 39650 && earfcn <= 41589)) return "2500";
        return band > 0 ? "B" + band : String.valueOf(earfcn);
    }

    private String getNrFreqName(int nrarfcn, int band) {
        if (band == 28 || (nrarfcn >= 151600 && nrarfcn <= 160600)) return "700";
        if (band == 78 || (nrarfcn >= 620000 && nrarfcn <= 653333)) return "3500";
        if (band == 77 || (nrarfcn > 653333 && nrarfcn <= 680000)) return "3700";
        if (band == 41 || (nrarfcn >= 499200 && nrarfcn <= 537999)) return "2500";
        if (band == 40 || (nrarfcn >= 460000 && nrarfcn <= 480000)) return "2300";
        if (band == 1 || (nrarfcn >= 422000 && nrarfcn <= 434000)) return "2100";
        if (band == 3 || (nrarfcn >= 361000 && nrarfcn <= 376000)) return "1800";
        if (band == 8 || (nrarfcn >= 185000 && nrarfcn <= 192000)) return "900";
        return band > 0 ? "n" + band : String.valueOf(nrarfcn);
    }

    private boolean is5gHighBand(int nrarfcn, int band) {
        return band == 78 || band == 77 || band == 41 || band == 40 ||
               (nrarfcn >= 460000 && nrarfcn <= 680000);
    }

    private String formatTaDistance(int ta, boolean isHighBand5G) {
        if (ta < 0 || ta >= 1282) return "N/A";
        double metersPerTa = isHighBand5G ? 39.0625 : 78.125;
        long meters = Math.round(ta * metersPerTa);
        if (meters >= 1000) {
            return String.format("%.2f km", meters / 1000.0);
        }
        return meters + " m";
    }

    private String getBandwidthForMccMnc(String mcc, String mnc, boolean is5G, int fallbackKhz) {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED && subscriptionManager != null) {
                List<SubscriptionInfo> subs = subscriptionManager.getActiveSubscriptionInfoList();
                if (subs != null) {
                    for (SubscriptionInfo sub : subs) {
                        String subMcc = String.valueOf(sub.getMcc());
                        String subMnc = String.valueOf(sub.getMnc());
                        if ((mcc != null && mcc.equals(subMcc)) || (mnc != null && mnc.equals(subMnc))) {
                            TelephonyManager tmSub = telephonyManager.createForSubscriptionId(sub.getSubscriptionId());
                            ServiceState ss = tmSub.getServiceState();
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
        if (fallbackKhz > 0 && fallbackKhz < 1000000) {
            return (fallbackKhz / 1000) + " MHz";
        }
        return is5G ? "100 MHz" : "10 MHz";
    }

    private GradientDrawable createCardBackground(int rsrp) {
        int glowColor;
        if (rsrp >= -85) {
            glowColor = Color.parseColor("#552E6B3E"); // Strong Green Glow
        } else if (rsrp >= -95) {
            glowColor = Color.parseColor("#554A6321"); // Good Olive Glow
        } else if (rsrp >= -105) {
            glowColor = Color.parseColor("#555A4D1E"); // Fair Warm Gold Glow
        } else if (rsrp >= -115) {
            glowColor = Color.parseColor("#554E3A1E"); // Weak Warm Amber Glow
        } else {
            glowColor = Color.parseColor("#55542424"); // Poor Muted Red Glow
        }
        int midColor = Color.parseColor("#16191D");
        int baseColor = Color.parseColor("#101215");

        GradientDrawable gd = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{glowColor, midColor, baseColor}
        );
        gd.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        return gd;
    }

    private int getDotColor(int rsrp) {
        if (rsrp >= -90) return Color.parseColor("#4CAF50"); // Green
        if (rsrp >= -105) return Color.parseColor("#FFA726"); // Orange
        return Color.parseColor("#EF5350"); // Red
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

        mainContainer.removeAllViews();

        try {
            List<CellInfo> allCells = telephonyManager.getAllCellInfo();
            if (allCells == null || allCells.isEmpty()) {
                TextView empty = new TextView(this);
                empty.setText("No cell info available. Ensure Location / GPS is ON.");
                empty.setTextColor(Color.parseColor("#9AA0A6"));
                empty.setPadding(dp(16), dp(24), dp(16), dp(16));
                mainContainer.addView(empty);
                return;
            }

            List<CellInfo> servingCells = new ArrayList<>();
            List<CellInfo> neighborCells = new ArrayList<>();

            for (CellInfo c : allCells) {
                if (c.isRegistered()) {
                    servingCells.add(c);
                } else {
                    neighborCells.add(c);
                }
            }

            // 1. RENDER SERVING CELLS (TOP CARDS)
            for (CellInfo cell : servingCells) {
                String raw = cell.toString();

                if (cell instanceof CellInfoLte) {
                    CellInfoLte lte = (CellInfoLte) cell;
                    CellIdentityLte id = lte.getCellIdentity();
                    CellSignalStrengthLte sig = lte.getCellSignalStrength();

                    int ci = id.getCi();
                    if (ci == 268435455 || ci == -1 || ci == Integer.MAX_VALUE) continue;

                    int enb = ci >> 8;
                    int cid = ci & 0xFF;
                    int tac = id.getTac();
                    int pci = id.getPci();
                    int earfcn = id.getEarfcn();
                    int band = (id.getBands() != null && id.getBands().length > 0) ? id.getBands()[0] : extractInt(raw, "mBands=\\[(\\d+)", 0);

                    String mcc = id.getMccString();
                    String mnc = id.getMncString();
                    String alpha = id.getOperatorAlphaLong() != null ? id.getOperatorAlphaLong().toString() : "";
                    String opName = getOperatorName(mcc, mnc, alpha);
                    String freqName = getLteFreqName(earfcn, band);
                    String bw = getBandwidthForMccMnc(mcc, mnc, false, id.getBandwidth());

                    int rsrp = sig.getRsrp();
                    int rsrq = sig.getRsrq();
                    int rssi = extractInt(raw, "rssi=(-?\\d+)", -105);
                    int rssnr = extractInt(raw, "rssnr=(-?\\d+)", sig.getRssnr());
                    int ta = extractInt(raw, "ta=(-?\\d+)", sig.getTimingAdvance());

                    LinearLayout card = new LinearLayout(this);
                    card.setOrientation(LinearLayout.VERTICAL);
                    card.setPadding(dp(18), dp(18), dp(18), dp(16));
                    card.setBackground(createCardBackground(rsrp));

                    card.addView(createOperatorHeader(opName, (mcc != null ? mcc : "") + " " + (mnc != null ? mnc : "")));
                    card.addView(createTechSubtitle("⚓  4G  |  LTE " + freqName + "  |  " + bw));

                    card.addView(createGridRow("CI", String.valueOf(ci), "EARFCN", String.valueOf(earfcn)));
                    card.addView(createGridRow("eNb:CID", enb + ":" + cid, "RSSI", (rssi != -1 ? rssi + " dBm" : "N/A")));
                    card.addView(createGridRow("TAC", String.valueOf(tac), "RSRP", rsrp + " dBm"));
                    card.addView(createGridRow("PCI", String.valueOf(pci), "RSRQ", rsrq + " dB"));

                    String snrStr = (rssnr != CellInfo.UNAVAILABLE && rssnr != Integer.MAX_VALUE && rssnr > -50 && rssnr < 60) ? rssnr + " dB" : "N/A";
                    card.addView(createGridRow("BW", bw, "SNR", snrStr));

                    if (ta >= 0 && ta < 1282) {
                        card.addView(createGridRow("TA", ta + "  |  " + formatTaDistance(ta, false), "", ""));
                    }

                    card.addView(createBadgeFooter(opName.substring(0, 1) + " | " + freqName, "Tap to add location"));
                    mainContainer.addView(card);
                    mainContainer.addView(createDivider());

                } else if (cell instanceof CellInfoNr) {
                    CellInfoNr nr = (CellInfoNr) cell;
                    CellIdentityNr id = (CellIdentityNr) nr.getCellIdentity();
                    CellSignalStrengthNr sig = (CellSignalStrengthNr) nr.getCellSignalStrength();

                    long nci = id.getNci();
                    long gnb = nci >> 12;
                    long cid = nci & 0xFFF;
                    int tac = id.getTac();
                    int pci = id.getPci();
                    int arfcn = id.getNrarfcn();
                    int band = (id.getBands() != null && id.getBands().length > 0) ? id.getBands()[0] : extractInt(raw, "mBands\\s*=\\s*\\[(\\d+)", 0);

                    String mcc = id.getMccString();
                    String mnc = id.getMncString();
                    String alpha = id.getOperatorAlphaLong() != null ? id.getOperatorAlphaLong().toString() : "";
                    String opName = getOperatorName(mcc, mnc, alpha);
                    String freqName = getNrFreqName(arfcn, band);
                    String bw = getBandwidthForMccMnc(mcc, mnc, true, -1);

                    int ssRsrp = sig.getSsRsrp();
                    int ssRsrq = sig.getSsRsrq();
                    int ssSinr = sig.getSsSinr();
                    int ta = extractInt(raw, "timingAdvance\\s*=\\s*(-?\\d+)", -1);
                    boolean highBand = is5gHighBand(arfcn, band);

                    LinearLayout card = new LinearLayout(this);
                    card.setOrientation(LinearLayout.VERTICAL);
                    card.setPadding(dp(18), dp(18), dp(18), dp(16));
                    card.setBackground(createCardBackground(ssRsrp));

                    card.addView(createOperatorHeader(opName, (mcc != null ? mcc : "") + " " + (mnc != null ? mnc : "")));
                    card.addView(createTechSubtitle("⚓  5G  |  SA " + freqName));

                    card.addView(createGridRow("CI", String.valueOf(nci), "ARFCN", String.valueOf(arfcn)));
                    card.addView(createGridRow("gNB:CID", gnb + ":" + cid, "S-RSRP", ssRsrp + " dBm"));
                    card.addView(createGridRow("TAC", String.valueOf(tac), "S-RSRQ", ssRsrq + " dB"));
                    card.addView(createGridRow("PCI", String.valueOf(pci), "S-SNR", (ssSinr != CellInfo.UNAVAILABLE && ssSinr != Integer.MAX_VALUE ? ssSinr + " dB" : "N/A")));
                    card.addView(createGridRow("BW", bw, "TA", (ta >= 0 && ta < 1282) ? (ta + "  |  " + formatTaDistance(ta, highBand)) : "N/A"));

                    mainContainer.addView(card);
                    mainContainer.addView(createDivider());
                }
            }

            // 2. RENDER NEIGHBOR CELLS LIST (BOTTOM ROWS)
            for (CellInfo cell : neighborCells) {
                String raw = cell.toString();

                if (cell instanceof CellInfoNr) {
                    CellInfoNr nr = (CellInfoNr) cell;
                    CellIdentityNr id = (CellIdentityNr) nr.getCellIdentity();
                    CellSignalStrengthNr sig = (CellSignalStrengthNr) nr.getCellSignalStrength();

                    int pci = id.getPci();
                    int arfcn = id.getNrarfcn();
                    int band = (id.getBands() != null && id.getBands().length > 0) ? id.getBands()[0] : extractInt(raw, "mBands\\s*=\\s*\\[(\\d+)", 0);
                    String freqName = getNrFreqName(arfcn, band);

                    int rsrp = sig.getSsRsrp();
                    int rsrq = sig.getSsRsrq();
                    int sinr = sig.getSsSinr();
                    String sinrStr = (sinr != CellInfo.UNAVAILABLE && sinr != Integer.MAX_VALUE && sinr >= -50 && sinr <= 50) ? String.valueOf(sinr) : "-";

                    mainContainer.addView(createNeighborRow("J | " + freqName, String.valueOf(pci), String.valueOf(arfcn), rsrp, rsrq, sinrStr));
                    mainContainer.addView(createThinDivider());

                } else if (cell instanceof CellInfoLte) {
                    CellInfoLte lte = (CellInfoLte) cell;
                    CellIdentityLte id = lte.getCellIdentity();
                    CellSignalStrengthLte sig = lte.getCellSignalStrength();

                    int pci = id.getPci();
                    int earfcn = id.getEarfcn();
                    if (pci < 0 || earfcn < 0) continue;

                    int band = (id.getBands() != null && id.getBands().length > 0) ? id.getBands()[0] : extractInt(raw, "mBands=\\[(\\d+)", 0);
                    String freqName = getLteFreqName(earfcn, band);
                    String prefix = (earfcn == 9560 || earfcn == 415) ? "B" : "L";

                    int rsrp = sig.getRsrp();
                    int rsrq = sig.getRsrq();
                    int sinr = extractInt(raw, "rssnr=(-?\\d+)", sig.getRssnr());
                    String sinrStr = (sinr != CellInfo.UNAVAILABLE && sinr != Integer.MAX_VALUE && sinr > -50 && sinr < 50 && sinr != 0) ? String.valueOf(sinr) : "-";

                    mainContainer.addView(createNeighborRow(prefix + " | " + freqName, String.valueOf(pci), String.valueOf(earfcn), rsrp, rsrq, sinrStr));
                    mainContainer.addView(createThinDivider());
                }
            }

        } catch (SecurityException ignored) {}
    }

    p
