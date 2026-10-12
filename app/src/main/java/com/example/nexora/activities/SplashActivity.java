package com.example.nexora.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nexora.R;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2200;

    private ImageView ivLogo;
    private TextView tvLogo;
    private TextView tvTagline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);

        initializeViews();
        startAnimations();
        moveToLogin();
    }

    private void initializeViews() {

        ivLogo = findViewById(R.id.ivLogo);
        tvLogo = findViewById(R.id.tvLogo);
        tvTagline = findViewById(R.id.tvTagline);
    }

    private void startAnimations() {

        Animation iconAnimation =
                AnimationUtils.loadAnimation(this, R.anim.icon_enter);

        Animation textAnimation =
                AnimationUtils.loadAnimation(this, R.anim.text_enter);

        Animation taglineAnimation =
                AnimationUtils.loadAnimation(this, R.anim.tagline_enter);

        ivLogo.startAnimation(iconAnimation);

        tvLogo.setAlpha(0f);
        tvTagline.setAlpha(0f);

        tvLogo.postDelayed(() -> {
            tvLogo.startAnimation(textAnimation);
        }, 300);

        tvTagline.postDelayed(() -> {
            tvTagline.startAnimation(taglineAnimation);
        }, 700);
    }

    private void moveToLogin() {

        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            Intent intent =
                    new Intent(SplashActivity.this, LoginActivity.class);

            startActivity(intent);

            finish();

        }, SPLASH_DURATION);
    }
}