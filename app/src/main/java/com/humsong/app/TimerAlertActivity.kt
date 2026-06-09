package com.humsong.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color
import android.view.Gravity
import android.view.WindowManager

class TimerAlertActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val itemId = intent.getStringExtra(TrainingTimerAlarmReceiver.EXTRA_TRAINING_ITEM_ID).orEmpty()
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        alarmVibrate(repeat = true)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            setBackgroundColor(Color.rgb(16, 17, 23))
        }
        val title = TextView(this).apply {
            text = "训练计时结束"
            textSize = 30f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        val subtitle = TextView(this).apply {
            text = "休息时间到了，准备继续训练。"
            textSize = 16f
            setTextColor(Color.rgb(198, 205, 216))
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 36)
        }
        val button = Button(this).apply {
            text = "返回训练"
            textSize = 18f
            setOnClickListener {
                if (itemId.isNotBlank()) {
                    getSharedPreferences(TRAINING_TIMER_PREFS, MODE_PRIVATE)
                        .edit()
                        .putString(KEY_PENDING_TRAINING_HIGHLIGHT_ITEM_ID, itemId)
                        .apply()
                }
                cancelAlarmVibration()
                startActivity(
                    Intent(this@TimerAlertActivity, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                )
                finish()
            }
        }
        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(button)
        setContentView(layout)
    }

    override fun onDestroy() {
        cancelAlarmVibration()
        super.onDestroy()
    }
}
