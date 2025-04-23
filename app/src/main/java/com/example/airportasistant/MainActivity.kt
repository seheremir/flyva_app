package com.example.airportasistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private lateinit var btnStartCamera: Button
    private lateinit var airportFromInput: EditText
    private lateinit var airportToInput: EditText
    private lateinit var gateInput: EditText
    private lateinit var flightTimeInput: EditText
    private lateinit var saveFlightInfoButton: Button
    private lateinit var remainingTimeText: TextView
    private var countDownTimer: CountDownTimer? = null

    private val CHANNEL_ID = "flight_notification_channel"
    private val NOTIFICATION_ID = 1
    private val REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // View'ları tanımla
        btnStartCamera = findViewById(R.id.btnStartCamera)
        airportFromInput = findViewById(R.id.airportFromInput)
        airportToInput = findViewById(R.id.airportToInput)
        gateInput = findViewById(R.id.gateInput)
        flightTimeInput = findViewById(R.id.flightTimeInput)
        saveFlightInfoButton = findViewById(R.id.saveFlightInfoButton)
        remainingTimeText = findViewById(R.id.remainingTimeText)

        // Bildirim Kanalı Oluşturma
        createNotificationChannel()

        // Git butonu işlevi
        btnStartCamera.setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java))
        }

        // Kaydet butonu işlevi
        saveFlightInfoButton.setOnClickListener {
            startCountdown()
        }

        // Bildirim izni kontrolü (Android 13 ve sonrası)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE)
            }
        }
    }

    // Bildirim kanalını oluşturma
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Flight Notification Channel"
            val descriptionText = "Channel for flight countdown notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startCountdown() {
        val flightTimeStr = flightTimeInput.text.toString()

        if (flightTimeStr.isEmpty()) {
            Toast.makeText(this, "Lütfen kalkış saatini girin!", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val flightTimeOnly = dateFormat.parse(flightTimeStr)

            if (flightTimeOnly != null) {
                val now = Calendar.getInstance()

                // Uçuş zamanı için yeni Calendar objesi oluştur
                val flightCalendar = Calendar.getInstance().apply {
                    time = flightTimeOnly
                    set(Calendar.YEAR, now.get(Calendar.YEAR))
                    set(Calendar.MONTH, now.get(Calendar.MONTH))
                    set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH))

                    // Eğer uçuş saati geçtiyse ve ileri tarih olması gerekiyorsa (opsiyonel)
                    if (before(now)) {
                        add(Calendar.DAY_OF_MONTH, 1) // Ertesi güne al
                    }
                }

                val diff = flightCalendar.timeInMillis - now.timeInMillis

                countDownTimer?.cancel() // Önceki timer'ı durdur

                if (diff > 0) {
                    remainingTimeText.visibility = TextView.VISIBLE

                    countDownTimer = object : CountDownTimer(diff, 1000) {
                        override fun onTick(millisUntilFinished: Long) {
                            val hours = millisUntilFinished / (1000 * 60 * 60)
                            val minutes = (millisUntilFinished % (1000 * 60 * 60)) / (1000 * 60)
                            val seconds = (millisUntilFinished % (1000 * 60)) / 1000

                            remainingTimeText.text = "✈️ Uçuş başlamasına:\n$hours saat $minutes dakika $seconds saniye"

                            if (hours == 0L && minutes < 60) {
                                remainingTimeText.setTextColor(Color.RED)
                            } else {
                                remainingTimeText.setTextColor(Color.parseColor("#E53935"))
                            }

                            showFlightNotification(hours, minutes, seconds)
                        }

                        override fun onFinish() {
                            remainingTimeText.text = "🛫 Uçuş zamanı geldi!"
                            remainingTimeText.setTextColor(Color.GREEN)
                            showFlightNotification(0, 0, 0)
                        }
                    }.start()
                } else {
                    remainingTimeText.visibility = TextView.VISIBLE
                    remainingTimeText.text = "⏰ Uçuş zamanı geçmiş!"
                    remainingTimeText.setTextColor(Color.RED)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Geçersiz saat formatı! (HH:mm şeklinde girin)", Toast.LENGTH_SHORT).show()
        }
    }


    private fun showFlightNotification(hours: Long, minutes: Long, seconds: Long) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationText = if (hours == 0L && minutes == 0L && seconds == 0L) {
            "🛫 Uçuş Zamanı Geldi!"
        } else {
            "✈️ Uçuş başlamasına: $hours saat $minutes dakika $seconds saniye"
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_flight_takeoff) // Uçuş simgesi (R.drawable.ic_flight_takeoff'ı proje dosyanızda eklemelisiniz)
            .setContentTitle("Uçuş Zamanı Yaklaşıyor")
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    // Kullanıcı izinlere yanıt verdiğinde çağrılır
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Bildirim izni verildi!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Bildirim izni reddedildi.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        super.onDestroy()
    }
}
