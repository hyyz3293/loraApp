package com.lora.cn.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.blankj.utilcode.util.SPUtils;
import com.lora.cn.R;
import com.lora.cn.constant.SpConstant;

import java.util.Locale;

public class WelcomeActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY = 2000; // 3秒延时
    private ProgressBar progressBar;
    private TextView countdownText;
    private Button skipButton;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_welcome);

        // 隐藏ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        progressBar = findViewById(R.id.progress_bar);
        countdownText = findViewById(R.id.countdown_text);
        skipButton = findViewById(R.id.skip_button);

        // 设置跳过按钮点击事件
        skipButton.setOnClickListener(v -> {
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }
            enterMainApp();
        });

        // 创建倒计时器
        countDownTimer = new CountDownTimer(SPLASH_DELAY, 50) {
            @Override
            public void onTick(long millisUntilFinished) {
                int progress = (int) ((SPLASH_DELAY - millisUntilFinished) * 100 / SPLASH_DELAY);
                progressBar.setProgress(progress);
                countdownText.setText(String.format(Locale.getDefault(), "%.1fs", millisUntilFinished / 1000.0));
            }

            @Override
            public void onFinish() {
                progressBar.setProgress(100);
                countdownText.setText("0s");
                enterMainApp();
            }
        };

        // 启动倒计时
        countDownTimer.start();
    }

    private void enterMainApp() {
        boolean isLogin = SPUtils.getInstance().getBoolean(SpConstant.IS_LOGIN, false);
        Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
        if (!isLogin) {
            intent = new Intent(WelcomeActivity.this, LoginActivity.class);
        }
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onDestroy() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        super.onDestroy();
    }
}