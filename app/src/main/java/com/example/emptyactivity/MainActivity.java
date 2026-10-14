package com.example.androidtemplate

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var infoTextView: TextView
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var refreshRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        infoTextView = findViewById(R.id.txt_cell_info)
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE
            ), 101)
        }

        refreshRunnable = object : Runnable {
            override fun run() {
                updateCellMetrics()
                handler.postDelayed(this, 2000)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshRunnable)
    }

    private fun updateCellMetrics() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            infoTextView.text = "Permission required for cell telemetry."
            return
        }

        val sb = StringBuilder()
        sb.append("=== REAL-TIME TELEMETRY ===\n\n")

        try {
            val allCells = telephonyManager.allCellInfo
            if (allCells.isNullOrEmpty()) {
                sb.append("No cell info available. Turn on Location/GPS.\n")
            } else {
                for ((index, cell) in allCells.withIndex()) {
                    sb.append("--- Tower [${index + 1}] (${cell.javaClass.simpleName}) ---\n")
                    sb.append("Registered: ${cell.isRegistered}\n")
                    
                    when (cell) {
                        is CellInfoLte -> {
                            val identity = cell.cellIdentity
                            val signal = cell.cellSignalStrength
                            sb.append("PCI: ${identity.pci} | EARFCN: ${identity.earfcn}\n")
                            sb.append("TAC: ${identity.tac} | CI: ${identity.ci}\n")
                            sb.append("RSRP: ${signal.rsrp} dBm\n")
                            sb.append("RSRQ: ${signal.rsrq} dB\n")
                            sb.append("Timing Advance: ${signal.timingAdvance}\n")
                        }
                        is CellInfoNr -> {
                            val identity = cell.cellIdentity as CellIdentityNr
                            val signal = cell.cellSignalStrength as CellSignalStrengthNr
                            sb.append("NR PCI: ${identity.pci} | ARFCN: ${identity.nrarfcn}\n")
                            sb.append("TAC: ${identity.tac} | NCI: ${identity.nci}\n")
                            sb.append("SS-RSRP: ${signal.ssRsrp} dBm\n")
                            sb.append("SS-RSRQ: ${signal.ssRsrq} dB\n")
                        }
                        is CellInfoWcdma -> {
                            val identity = cell.cellIdentity
                            val signal = cell.cellSignalStrength
                            sb.append("PSC: ${identity.psc} | UARFCN: ${identity.uarfcn}\n")
                            sb.append("LAC: ${identity.lac} | CID: ${identity.cid}\n")
                            sb.append("RSSI: ${signal.dbm} dBm\n")
                        }
                        is CellInfoGsm -> {
                            val identity = cell.cellIdentity
                            val signal = cell.cellSignalStrength
                            sb.append("BSIC: ${identity.bsic} | ARFCN: ${identity.arfcn}\n")
                            sb.append("LAC: ${identity.lac} | CID: ${identity.cid}\n")
                            sb.append("RSSI: ${signal.dbm} dBm\n")
                        }
                    }
                    sb.append("\n")
                }
            }
        } catch (e: SecurityException) {
            sb.append("Security Exception reading cell info.\n")
        }

        infoTextView.text = sb.toString()
    }
}
