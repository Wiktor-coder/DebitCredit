package ru.github.debitcredit

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Устанавливаем тему ДО super.onCreate()
        setTheme(R.style.Theme_Splash1)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Задержка перед переходом в MainActivity
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 2000) // 2 секунды
    }
}