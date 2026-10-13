package com.example.bafangdiagnostic

import android.app.Activity
import android.os.Bundle
import android.hardware.usb.UsbManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.app.PendingIntent
import android.os.Build
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Read-only USB inventory. Does NOT claim to speak Bafang CAN or UART. */
class MainActivity : Activity() {
    private lateinit var output: TextView
    private val entries = mutableListOf<String>()
    private val permissionAction = "com.example.bafangdiagnostic.USB_PERMISSION"
    private val permissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == permissionAction) {
                val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                Toast.makeText(context, if (granted) "USB access granted" else "USB access denied", Toast.LENGTH_LONG).show()
                scanUsb()
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val filter = IntentFilter(permissionAction)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(permissionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(permissionReceiver, filter)
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 36, 30, 24)
        }
        val title = TextView(this).apply {
            text = "Bafang M420 Diagnostic"
            textSize = 23f
            setTypeface(null, Typeface.BOLD)
        }
        root.addView(title)
        root.addView(TextView(this).apply {
            text = "Android USB interface discovery · read-only prototype\nNo motor commands or parameter writes."
            textSize = 15f
            setPadding(0, 16, 0, 18)
        })
        val scan = Button(this).apply { text = "Scan connected USB devices"; setOnClickListener { scanUsb() } }
        root.addView(scan)
        val permission = Button(this).apply { text = "Request USB device permission"; setOnClickListener { requestUsbPermission() } }
        root.addView(permission)
        val listen = Button(this).apply { text = "Listen for data (10 s, read-only)"; setOnClickListener { listenForData() } }
        root.addView(listen)
        val export = Button(this).apply { text = "Share report"; setOnClickListener { shareReport() } }
        root.addView(export)
        output = TextView(this).apply { textSize = 14f; setTextIsSelectable(true); setPadding(0, 18, 0, 0) }
        root.addView(ScrollView(this).apply { addView(output) }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        scanUsb()
    }

    private fun scanUsb() {
        val manager = getSystemService(Context.USB_SERVICE) as UsbManager
        entries.clear()
        entries += "Scan: " + SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        entries += "USB host supported: " + packageManager.hasSystemFeature("android.hardware.usb.host")
        val devices = manager.deviceList.values.sortedBy { it.deviceName }
        if (devices.isEmpty()) entries += "No USB device detected. A USB-C-to-USB-A adapter alone does not enumerate as a USB device."
        devices.forEach { d: UsbDevice ->
            entries += "\nDevice: ${d.deviceName}"
            entries += "VID: 0x${d.vendorId.toString(16).padStart(4,'0')} / PID: 0x${d.productId.toString(16).padStart(4,'0')}"
            entries += "USB class: ${d.deviceClass}; interfaces: ${d.interfaceCount}; permission: ${manager.hasPermission(d)}"
            if (d.vendorId == 0x0323 && d.productId == 0x0627) {
                entries += "MATCH: Bafang BESST USB interface (VID 0323, PID 0627)"
                entries += "This confirms only the interface identity, not communication with the motor."
            } else {
                entries += "Not the documented Bafang BESST USB interface (0323:0627)."
            }
            for (i in 0 until d.interfaceCount) {
                val intf = d.getInterface(i)
                entries += "  Interface $i: class=${intf.interfaceClass} subclass=${intf.interfaceSubclass} protocol=${intf.interfaceProtocol} endpoints=${intf.endpointCount}"
                for (j in 0 until intf.endpointCount) {
                    val ep = intf.getEndpoint(j)
                    entries += "    Endpoint $j: address=0x${ep.address.toString(16)} type=${ep.type} direction=${ep.direction}"
                }
            }
        }
        entries += "\nMOTOR STATUS: NOT CONNECTED / NOT VERIFIED"
        entries += "This prototype cannot read M420 torque, cadence, error codes or CAN frames yet. A verified USB-CAN interface and its driver/protocol are required."
        entries += "Do not connect the phone directly to any motor pins. Do not ride a bicycle whose motor continues pushing unexpectedly."
        output.text = entries.joinToString("\n")
    }

    private fun requestUsbPermission() {
        val manager = getSystemService(Context.USB_SERVICE) as UsbManager
        val devices = manager.deviceList.values
        val device = devices.firstOrNull { it.vendorId == 0x0323 && it.productId == 0x0627 } ?: devices.firstOrNull()
        if (device == null) {
            Toast.makeText(this, "No USB device detected", Toast.LENGTH_LONG).show()
            return
        }
        val flags = if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
        val pendingIntent = PendingIntent.getBroadcast(this, 0, Intent(permissionAction).setPackage(packageName), flags)
        manager.requestPermission(device, pendingIntent)
    }

    /** Read-only probe: opens the device and reads every IN endpoint for 10 seconds. Never sends data. */
    private fun listenForData() {
        val manager = getSystemService(Context.USB_SERVICE) as UsbManager
        val device = manager.deviceList.values.firstOrNull()
        if (device == null) {
            appendLine("\nLISTEN: no USB device detected. If the phone is plugged into the bike display and nothing shows, that port is power only.")
            return
        }
        if (!manager.hasPermission(device)) {
            requestUsbPermission()
            Toast.makeText(this, "Allow access, then press Listen again", Toast.LENGTH_LONG).show()
            return
        }
        val conn = manager.openDevice(device)
        if (conn == null) {
            appendLine("\nLISTEN: device found but could not be opened.")
            return
        }
        appendLine("\nLISTEN: reading ${device.deviceName} for 10 seconds...")
        Thread {
            val readable = mutableListOf<Pair<UsbInterface, UsbEndpoint>>()
            val claimed = mutableListOf<UsbInterface>()
            for (i in 0 until device.interfaceCount) {
                val intf = device.getInterface(i)
                if (!conn.claimInterface(intf, true)) continue
                claimed += intf
                for (j in 0 until intf.endpointCount) {
                    val ep = intf.getEndpoint(j)
                    if (ep.direction == UsbConstants.USB_DIR_IN && ep.type != UsbConstants.USB_ENDPOINT_XFER_ISOC) readable += intf to ep
                }
            }
            if (readable.isEmpty()) {
                runOnUiThread { appendLine("LISTEN: no readable data endpoints on this device.") }
            } else {
                val buf = ByteArray(512)
                var total = 0
                val end = System.currentTimeMillis() + 10_000
                while (System.currentTimeMillis() < end && total < 8192) {
                    for ((_, ep) in readable) {
                        val n = conn.bulkTransfer(ep, buf, buf.size, 200)
                        if (n > 0) {
                            total += n
                            val hex = buf.take(n).joinToString(" ") { "%02X".format(it) }
                            val ascii = buf.take(n).map { b -> val c = b.toInt() and 0xFF; if (c in 32..126) c.toChar() else '.' }.joinToString("")
                            val epAddr = ep.address
                            runOnUiThread { appendLine("EP 0x${epAddr.toString(16)} [$n]: $hex\n  $ascii") }
                        }
                    }
                }
                val t = total
                runOnUiThread { appendLine(if (t > 0) "LISTEN: done, $t bytes received. Data IS coming through." else "LISTEN: done, 0 bytes received in 10 seconds.") }
            }
            claimed.forEach { conn.releaseInterface(it) }
            conn.close()
        }.start()
    }

    private fun appendLine(line: String) {
        entries += line
        output.text = entries.joinToString("\n")
    }

    override fun onDestroy() {
        unregisterReceiver(permissionReceiver)
        super.onDestroy()
    }

    private fun shareReport() {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_SUBJECT, "M420 USB diagnostic inventory")
            putExtra(android.content.Intent.EXTRA_TEXT, entries.joinToString("\n"))
        }
        startActivity(android.content.Intent.createChooser(intent, "Share USB report"))
    }
}
