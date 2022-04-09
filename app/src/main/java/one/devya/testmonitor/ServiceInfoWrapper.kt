package one.devya.testmonitor

import android.net.nsd.NsdServiceInfo
import android.os.Build
import androidx.annotation.RequiresApi

internal class ServiceInfoWrapper(private val _info: NsdServiceInfo) {
    val address: String
        @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
        get() = _info.host.hostAddress
    val port: Int
        @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
        get() = _info.port
    val name: String
        @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
        get() = _info.serviceName.replace("\\\\032", " ").replace("\\032", " ")

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun toString(): String {
        return name
    }
}