package org.dbtag.driver

import android.content.Context
import android.location.Location
import android.net.wifi.WifiManager
import android.os.Build
import android.text.format.Formatter


class AssetCheckIn(private val asset: String, private val context: Context, private val batteryLevel: Int) {
    private val hw = (Build.MANUFACTURER + "." + Build.BRAND + "." + Build.MODEL + ".Android." + Build.VERSION.RELEASE).replace(" ", "")
    private val ver = context.packageManager.getPackageInfo(context.packageName, 0).versionName

    fun asMessage(location: Location?, lastPing: Long): String {
        var text = "#sys.asset.$asset"
        val wifiInfo = (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).connectionInfo
        var ssid = wifiInfo.ssid
        ssid = if (ssid == null) "" else ssid.replace("\"", "")
        if (ssid == "<unknown ssid>")
            ssid = "Hotspot"
        if (ssid.isNotEmpty())
            text += " #sys.ssid.$ssid"
        text += " #wifi=" + Math.round((5 * (wifiInfo.rssi - -100) / (-55 - -100)).toFloat())  // 0 to 4
        text += " #bat=$batteryLevel" // battery is 0 - 4, plus another 10 if charging
        text += " #ping=$lastPing"
        @Suppress("DEPRECATION")
        text += " #sys.ip." + Formatter.formatIpAddress(wifiInfo.ipAddress)
        text += " #sys.hw.$hw"
        text += " #sys.ver.$ver"

        location?.run {
            text += " #lat=" + "%.7f".format(latitude) + " #lon=" + "%.7f".format(longitude)
        }
        return text
    }
}