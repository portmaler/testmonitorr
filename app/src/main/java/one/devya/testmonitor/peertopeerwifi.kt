package one.devya.testmonitor


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import kotlin.properties.Delegates


class peertopeerwifi : AppCompatActivity() {

    lateinit var wifimanager : WifiManager

    var btnOnOff : Button? = null
    var btnDiscover : Button? = null
    var btnSend : Button? = null
    var listView : ListView? = null
    lateinit var read_msg_box : TextView
    var connectionStatus : TextView? = null
    var writeMsg : EditText? = null

    var isHost by Delegates.notNull<Boolean>()
    var serverClass: ServerClass? = null
    var clientClass: ClientClass? = null



    private var manager: WifiP2pManager by Delegates.notNull()
    private var channel: WifiP2pManager.Channel by Delegates.notNull()
    //private lateinit var manager : WifiP2pManager
    //private lateinit var channel : WifiP2pManager.Channel
    private lateinit var receiver : BroadcastReceiver
    lateinit var intentFilter : IntentFilter
    private val peers = mutableListOf<WifiP2pDevice>()
    private val deviceArray = mutableListOf<WifiP2pDevice>()
    private val deviceNameArray = mutableListOf<String>()
   // lateinit var deviceNameArray : Array<String>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_peertopeerwifi)

        initialwork()
        exqListener()
    }

    private fun initialwork() {
        btnOnOff = findViewById<Button>(R.id.onOff)
        btnDiscover = findViewById<Button>(R.id.discover)
        btnSend = findViewById<Button>(R.id.sendButton)
        listView = findViewById<ListView>(R.id.peerListView)
        read_msg_box = findViewById<TextView>(R.id.readMsg)
        connectionStatus = findViewById<TextView>(R.id.connectionStatus)
        writeMsg = findViewById<EditText>(R.id.writeMsg)

        wifimanager =  applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        manager = (getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager?)!!
        channel = manager?.initialize(this, mainLooper, null)
        receiver = WiFiDirectBroadcastReceiver(manager,channel,this)

        channel?.also { channel ->
            receiver = manager?.let { WiFiDirectBroadcastReceiver(it, channel, this) }
        }

        intentFilter = IntentFilter()
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)


        /*intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }*/
    }

    val peerListListener = WifiP2pManager.PeerListListener { peerList ->
        val refreshedPeers = peerList.deviceList
        if (refreshedPeers != peers) {
            peers.clear()
            peers.addAll(refreshedPeers)

            //deviceNameArray = Array<String>(peerList.deviceList.size){""}
            var index = 0
            for(device in refreshedPeers){
                deviceNameArray.set(index, device.deviceName)
                deviceArray[index] = device
                index++
            }

            val adapter = ArrayAdapter(this, R.layout.listview_item , deviceNameArray)
            listView!!.adapter = adapter

            // If an AdapterView is backed by this data, notify it
            // of the change. For instance, if you have a ListView of
            // available peers, trigger an update.
         //   //(listAdapter as WiFiPeerListAdapter).notifyDataSetChanged()

            // Perform any other updates needed based on the new list of
            // peers connected to the Wi-Fi P2P network.
        }

        if (peers.isEmpty()) {
            Toast.makeText(this@peertopeerwifi,"no device found",Toast.LENGTH_SHORT).show()
            return@PeerListListener
        }
    }


    val connectionListener = WifiP2pManager.ConnectionInfoListener { info ->

        // String from WifiP2pInfo struct
        val groupOwnerAddress: String = info.groupOwnerAddress.hostAddress

        // After the group negotiation, we can determine the group owner
        // (server).
        if (info.groupFormed && info.isGroupOwner) {
            // Do whatever tasks are specific to the group owner.
            // One common case is creating a group owner thread and accepting
            // incoming connections.
            connectionStatus?.setTag("Host")
            isHost = true
            serverClass = ServerClass()
            serverClass!!.start()
        } else if (info.groupFormed) {
            // The other device acts as the peer (client). In this case,
            // you'll want to create a peer thread that connects
            // to the group owner.
            connectionStatus?.setTag("Client")
            isHost = false
            clientClass = ClientClass(groupOwnerAddress)
            clientClass!!.start()
        }
    }



    private fun exqListener() {
        btnOnOff?.setOnClickListener {

            val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
            startActivityForResult(intent,1)
            if(wifimanager.isWifiEnabled) {
                wifimanager.isWifiEnabled = false
                btnOnOff!!.setText(" Wifi ON")
            }
            else{
                wifimanager.setWifiEnabled(true)
                btnOnOff!!.setText("Wifi OFF")
            }
        }

        btnDiscover?.setOnClickListener {
            manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {

                override fun onSuccess() {
                    connectionStatus!!.setText("Discovry started")
                }

                override fun onFailure(reasonCode: Int) {
                    connectionStatus!!.setText("Discovry started failed")
                }
            })
        }

        listView?.setOnItemClickListener { adapterView, view, i, l ->
            val device : WifiP2pDevice = deviceArray[i]
            val config = WifiP2pConfig().apply {
                deviceAddress = device.deviceAddress
                wps.setup = WpsInfo.PBC
            }

            manager.connect(channel, config, object : WifiP2pManager.ActionListener {

                override fun onSuccess() {
                    Toast.makeText(this@peertopeerwifi,"connect to " + device.deviceName,Toast.LENGTH_SHORT).show()
                }

                override fun onFailure(reason: Int) {
                    Toast.makeText(
                        this@peertopeerwifi,
                        "Connect failed. Retry.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })

        }

        btnSend?.setOnClickListener {
            val executor = Executors.newSingleThreadExecutor()
            val msg = writeMsg?.text.toString().toByteArray()

            executor.execute {
                if(msg != null && isHost)
                    run {
                        serverClass?.writeMessage(msg)
                    }
                else(msg != null && !isHost)
                run {
                    clientClass?.writeMessage(msg)
                }
            }
        }




    }


    override fun onResume() {
        super.onResume()
        receiver?.also { receiver ->
            registerReceiver(receiver, intentFilter)
        }
    }

    override fun onPause() {
        super.onPause()
        receiver?.also { receiver ->
            unregisterReceiver(receiver)
        }
    }

    inner class ClientClass(
        private val hostAddress: String,

        ) : Thread(){
        lateinit var inputStream : InputStream
        lateinit var outputStream : OutputStream
        private val socket: Socket = Socket()
        var hostAdd: String = hostAddress

        fun writeMessage(bytes : ByteArray) {

            try {
                outputStream.write(bytes)

            }
            catch(e: IOException) { e.printStackTrace()}
        }



        override fun run() {
            super.run()

            try {
                socket.connect(InetSocketAddress(hostAdd, 1810), 2000)
                inputStream = socket.getInputStream()
                outputStream = socket.getOutputStream()
            }
            catch(e: Exception){
                e.printStackTrace()
            }

            val executor = Executors.newSingleThreadExecutor()
            val handler = Handler(Looper.getMainLooper())

            executor.execute {
                val buffer = ByteArray(8192)
                var bytes: Int
                while (true) {
                    try {
                        bytes = inputStream.read(buffer)

                        if (bytes > 0) {
                            val finalBytes = bytes
                            handler.post {
                                val textMessage = String(buffer, 0, finalBytes)
                                read_msg_box.setText(textMessage)
                            }
                        }
                    }
                    catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    inner class ServerClass() : Thread(){

        lateinit var serverSocket: ServerSocket
        lateinit var inputStream: InputStream
        lateinit var outputStream: OutputStream
        private var socket: Socket = Socket()

        fun writeMessage(bytes : ByteArray) {

            try {
                outputStream.write(bytes)

            }
            catch(e: IOException) { e.printStackTrace()}
        }


        override fun run() {
            super.run()

            try{
                serverSocket = ServerSocket(1810)
                socket = serverSocket.accept()
                inputStream = socket.getInputStream()
                outputStream = socket.getOutputStream()
            }
            catch (e: Exception) {
                e.printStackTrace()
            }

            val executor = Executors.newSingleThreadExecutor()
            val handler = Handler(Looper.getMainLooper())

            executor.execute {
                val buffer = ByteArray(8192)
                var bytes: Int
                while (true) {
                    try {
                        bytes = inputStream.read(buffer)

                        if (bytes > 0) {
                            val finalBytes = bytes
                            handler.post {
                                val textMessage = String(buffer, 0, finalBytes)
                                read_msg_box.setText(textMessage)
                            }
                        }
                    }
                    catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }



}


