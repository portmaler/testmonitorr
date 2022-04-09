package one.devya.testmonitor

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val wifidirectcard : CardView = findViewById(R.id.card4)
        wifidirectcard.setOnClickListener {
            val intent = Intent(this,peertopeerwifi::class.java)
            startActivity(intent)
            finish()
        }

        val parentcard : CardView = findViewById(R.id.card1)
        parentcard.setOnClickListener {
            val intent = Intent(this,ParentActivity::class.java)
            startActivity(intent)
            finish()
        }

        val childcard : CardView = findViewById(R.id.card2)
        childcard.setOnClickListener {
            val intent = Intent(this,ChildActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}