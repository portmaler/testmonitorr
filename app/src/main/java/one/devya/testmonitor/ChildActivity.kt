package one.devya.testmonitor

import android.content.Context
import android.media.AudioRecord
import android.net.nsd.NsdManager
import android.net.nsd.NsdManager.RegistrationListener
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.text.format.Formatter
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

class ChildActivity : AppCompatActivity() {

    val TAG = "BabyMonitor"
    var _nsdManager: NsdManager? = null
    var _registrationListener: RegistrationListener? = null
    var _serviceThread: Thread? = null

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_child)
        Log.i("BabyMonitor", "Baby monitor start")
        _nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager

        _serviceThread = Thread {
            var e: IOException?
            while (!Thread.currentThread().isInterrupted) {
                var serverSocket: ServerSocket? = null
                var socket: Socket
                try {
                    val serverSocket2 = ServerSocket(0)
                    try {
                        this@ChildActivity.registerService(serverSocket2.localPort)
                        socket = serverSocket2.accept()
                        Log.i("BabyMonitor", "Connection from parent device received")
                        serverSocket2.close()
                        serverSocket = null
                        this@ChildActivity.unregisterService()
                        this@ChildActivity.serviceConnection(socket)
                        socket.close()
                    } catch (e2: IOException) {
                        e = e2
                        serverSocket = serverSocket2
                        Log.e("BabyMonitor", "Connection failed", e)
                        if (serverSocket != null) {
                        }
                    }
                } catch (e3: IOException) {
                    e = e3
                    Log.e("BabyMonitor", "Connection failed", e)
                    if (serverSocket != null) {
                    }
                } catch (th: Throwable) {
                    th.fillInStackTrace()
                }
                if (serverSocket != null) {
                    try {
                        serverSocket.close()
                    } catch (e4: IOException) {
                        Log.e("BabyMonitor", "Failed to close stray connection", e4)
                    }
                }
            }
        }
        _serviceThread!!.start()
        runOnUiThread {
            val addressText = this@ChildActivity.findViewById<View>(R.id.address) as TextView
            val address = (applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).connectionInfo.ipAddress
            if (address != 0) {
                addressText.text = Formatter.formatIpAddress(address)
            } else {
                addressText.setText(R.string.wifiNotConnected)
            }
        }
    }

    //*****************service connection hh **********

    @Throws(IOException::class)
    private fun serviceConnection(socket: Socket) {
        runOnUiThread {
            (this@ChildActivity.findViewById<View>(R.id.textStatus) as TextView).setText(
                R.string.streaming
            )
        }
        val bufferSize = AudioRecord.getMinBufferSize(11025, 16, 2)
        val audioRecord = AudioRecord(1, 11025, 16, 2, bufferSize)
        val byteBufferSize = bufferSize * 2
        val buffer = ByteArray(byteBufferSize)
        try {
            audioRecord.startRecording()
            val out = socket.getOutputStream()
            socket.sendBufferSize = byteBufferSize
            Log.d("BabyMonitor", "Socket send buffer size: " + socket.sendBufferSize)
            while (socket.isConnected && !Thread.currentThread().isInterrupted) {
                out.write(buffer, 0, audioRecord.read(buffer, 0, bufferSize))
            }
            audioRecord.stop()
        } catch (th: Throwable) {
            audioRecord.stop()
        }
    }

    //*************REGISTERsERVICE/***************

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    private fun registerService(port: Int) {
        // Create the NsdServiceInfo object, and populate it.
        val serviceInfo = NsdServiceInfo().apply {
            // The name is subject to change based on conflicts
            // with other services advertised on the same network.
            serviceName = "BabyMonitor"
            serviceType = "_babymonii._tcp"
            setPort(port)
        }
        _registrationListener = object : RegistrationListener {

            @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
            override fun onServiceRegistered(nsdServiceInfo: NsdServiceInfo) {
                // Save the service name. Android may have changed it in order to
                // resolve a conflict, so update the name you initially requested
                // with the name Android actually used.
                val serviceName: String = nsdServiceInfo.getServiceName()
                Log.i("BabyMonitor", "Service name: $serviceName")
                this@ChildActivity.runOnUiThread {
                    (this@ChildActivity.findViewById<View>(R.id.textStatus) as TextView).setText(R.string.waitingForParent)
                    (this@ChildActivity.findViewById<View>(R.id.textService) as TextView).text = serviceName
                    (this@ChildActivity.findViewById<View>(R.id.port) as TextView).text = Integer.toString(port)
                }
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                // Registration failed! Put debugging code here to determine why.
                Log.e("BabyMonitor", "Registration failed: $errorCode")
            }

            override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                // Service has been unregistered. This only happens when you call
                // NsdManager.unregisterService() and pass in this listener.
                Log.i("BabyMonitor", "Unregistering service")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                // Unregistration failed. Put debugging code here to determine why.
                Log.e("BabyMonitor", "Unregistration failed: $errorCode")
            }
        }
        _nsdManager?.apply {
            registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, _registrationListener)
        }
    }

    //*************REGISTERsERVICE/***************

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    private fun unregisterService() {
        if (_registrationListener != null) {
            Log.i("BabyMonitor", "Unregistering monitoring service")
            _nsdManager!!.unregisterService(_registrationListener)
            _registrationListener = null
        }
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun onDestroy() {
        Log.i("BabyMonitor", "Baby monitor stop")
        unregisterService()
        if (_serviceThread != null) {
            _serviceThread!!.interrupt()
            _serviceThread = null
        }
        super.onDestroy()
    }

}