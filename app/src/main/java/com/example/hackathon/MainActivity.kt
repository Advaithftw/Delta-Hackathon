package com.example.hackathon
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var webSocket: WebSocket
    private var selectedColor: Int = Color.RED
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            handleSignInResult(account)
        } else {
            signIn()
        }

        val gridLayout = findViewById<GridLayout>(R.id.gameboard)
        val buttonRed = findViewById<ImageButton>(R.id.button_red)
        val buttonBlue = findViewById<ImageButton>(R.id.button_blue)
        val buttonGreen = findViewById<ImageButton>(R.id.button_green)

        val tileSize = resources.displayMetrics.widthPixels / 5

        val tiles = ArrayList<TextView>()

        for (i in 0 until 25) {
            val tile = TextView(this)
            tile.layoutParams = GridLayout.LayoutParams().apply {
                width = tileSize
                height = tileSize
                setMargins(2, 2, 2, 2)
            }
            tile.setBackgroundColor(Color.WHITE)
            tile.setOnClickListener {
                tile.setBackgroundColor(selectedColor)
                sendColorChange(i, selectedColor)
            }
            gridLayout.addView(tile)
            tiles.add(tile)
        }

        buttonRed.setOnClickListener { selectedColor = Color.RED }
        buttonBlue.setOnClickListener { selectedColor = Color.BLUE }
        buttonGreen.setOnClickListener { selectedColor = Color.GREEN }

        val client = OkHttpClient.Builder()
            .readTimeout(3, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder().url("ws://localhost:8080").build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocket", "Connected")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WebSocket", "Received: $text")
                runOnUiThread {

                    val jsonObject = JSONObject(text)
                    val tileIndex = jsonObject.getInt("index")
                    val color = jsonObject.getInt("color")

                    tiles[tileIndex].setBackgroundColor(color)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "Error: ${t.message}")
            }
        })
    }

    private fun sendColorChange(index: Int, color: Int) {
        val json = JSONObject().apply {
            put("index", index)
            put("color", color)
        }
        webSocket.send(json.toString())
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                handleSignInResult(account)
            } catch (e: ApiException) {
                Log.w("Google Sign-In", "signInResult:failed code=" + e.statusCode)
            }
        }
    }

    private fun handleSignInResult(account: GoogleSignInAccount?) {
        account?.let {
            val email = it.email
            val displayName = it.displayName
            Log.d("Google Sign-In", "Signfed in as $displayName ($email)")
        }
    }
}
