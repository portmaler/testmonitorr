package one.devya.testmonitor

import android.content.Context
import android.content.Intent
import android.net.nsd.NsdManager
import android.net.nsd.NsdManager.DiscoveryListener
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity

class ParentActivity : AppCompatActivity() {

    val TAG = "BabyMonitor"
    var _discoveryListener: DiscoveryListener? = null
    var _nsdManager: NsdManager? = null

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent)
        _nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager
        (findViewById<View>(R.id.discoverChildButton) as Button).setOnClickListener { this@ParentActivity.loadDiscoveryViaMdns() }
        (findViewById<View>(R.id.enterChildAddressButton) as Button).setOnClickListener { this@ParentActivity.loadDiscoveryViaAddress() }
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    private fun loadDiscoveryViaMdns() {
        setContentView(R.layout.activity_discover_mdns)
        startServiceDiscovery("_babymonitor._tcp.")
    }

    private fun loadDiscoveryViaAddress() {
        setContentView(R.layout.activity_discover_address)
        (findViewById<View>(R.id.connectViaAddressButton) as Button).setOnClickListener(
            View.OnClickListener {
                Log.i("BabyMonitor", "Connecting to child device via address")
                val portField = this@ParentActivity.findViewById<View>(R.id.portField) as EditText
                val addressString =
                    (this@ParentActivity.findViewById<View>(R.id.ipAddressField) as EditText).text.toString()
                val portString = portField.text.toString()
                if (addressString.length == 0) {
                    Toast.makeText(
                        this@ParentActivity,
                        R.string.invalidAddress,
                        Toast.LENGTH_LONG
                    ).show()
                    return@OnClickListener
                }
                try {
                    this@ParentActivity.connectToChild(
                        addressString,
                        portString.toInt(),
                        addressString
                    )
                } catch (e: NumberFormatException) {
                    Toast.makeText(this@ParentActivity, R.string.invalidPort, Toast.LENGTH_LONG)
                        .show()
                }
            })
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    fun startServiceDiscovery(serviceType : String){
        val nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager
        val serviceTable = findViewById<View>(R.id.ServiceTable) as ListView
        val availableServicesAdapter = ArrayAdapter<Any?>(this, R.layout.available_children_list)
        serviceTable.setAdapter(availableServicesAdapter)
        serviceTable.setOnItemClickListener(OnItemClickListener { parent, view, position, id ->
            val info = parent.getItemAtPosition(position) as ServiceInfoWrapper
            this@ParentActivity.connectToChild(info.address, info.port, info.name)
        })

        _discoveryListener = object : NsdManager.DiscoveryListener {

            // Called as soon as service discovery begins.
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "Service discovery started")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                // A service was found! Do something with it.
                Log.d(TAG, "Service discovery success$service")
                when {
                    service.serviceType != serviceType -> // Service type is the string containing the protocol and
                        // transport layer for this service.
                        Log.d(TAG, "Unknown Service Type: ${service.serviceType}")
                    service.serviceName == "BabyMonitor" -> // The name of the service tells the user what they'd be
                        // connecting to. It could be "Bob's Chat App".
                        Log.d(TAG, "Same machine: BabyMonitor")
                    service.serviceName.contains("BabyMonitor") ->
                        this@ParentActivity._nsdManager?.resolveService(
                            service,
                            object : NsdManager.ResolveListener {
                                override fun onResolveFailed(
                                    serviceInfo: NsdServiceInfo,
                                    errorCode: Int
                                ) {
                                    Log.e(
                                        "BabyMonitor",
                                        "Resolve failed: error $errorCode for service: $serviceInfo"
                                    )
                                }

                                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                                    Log.i("BabyMonitor", "Resolve Succeeded: $serviceInfo")
                                    this@ParentActivity.runOnUiThread(Runnable {
                                        availableServicesAdapter.add(
                                            ServiceInfoWrapper(serviceInfo)
                                        )
                                    })
                                }
                            })
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Log.e(TAG, "service lost: $service")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.i(TAG, "Discovery stopped: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery failed: Error code:$errorCode")
                nsdManager.stopServiceDiscovery(this)
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery failed: Error code:$errorCode")
                nsdManager.stopServiceDiscovery(this)
            }
        }
    }

    private fun connectToChild(address: String, port: Int, name: String) {
        val i = Intent(applicationContext, ListenActivity::class.java)
        val b = Bundle()
        b.putString("address", address)
        b.putInt("port", port)
        b.putString("name", name)
        i.putExtras(b)
        startActivity(i)
    }
}