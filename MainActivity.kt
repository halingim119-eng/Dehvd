package com.webtoon.helper

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val l = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 96, 48, 48) }
        l.addView(TextView(this).apply {
            textSize = 16f
            text = "1. 아래 버튼 → 설치된 앱(또는 다운로드한 서비스) → '웹툰 도우미' 켜기\n" +
                   "2. 웹툰 앱을 열면 화면에 작은 바가 뜹니다 (≡ 잡고 드래그)\n" +
                   "3. ▶ 시작 / 슬라이더로 속도 조절 / 자동다음화 ON·OFF"
        })
        l.addView(Button(this).apply {
            text = "접근성 설정 열기"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
        setContentView(l)
    }
}
