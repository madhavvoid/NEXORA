package com.example.nexora.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nexora.R;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StudentAnnouncementsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ProgressBar progressBar;
    private TextView tvMessage;
    private LinearLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_announcements);

        progressBar = findViewById(R.id.progressStudentAnnouncements);
        tvMessage = findViewById(R.id.tvStudentAnnouncementsMessage);
        container = findViewById(R.id.studentAnnouncementsContainer);

        findViewById(R.id.btnStudentAnnouncementsBack)
                .setOnClickListener(v -> finish());

        db = FirebaseFirestore.getInstance();

        loadAnnouncements();
    }

    private void loadAnnouncements() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvMessage.setText("Loading announcements...");
        container.removeAllViews();

        db.collection("announcements")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    progressBar.setVisibility(View.GONE);
                    container.removeAllViews();

                    if (snapshot.isEmpty()) {
                        tvMessage.setVisibility(View.VISIBLE);
                        tvMessage.setText(
                                "There are no announcements yet."
                        );
                        return;
                    }

                    tvMessage.setVisibility(View.GONE);

                    for (DocumentSnapshot document :
                            snapshot.getDocuments()) {
                        addAnnouncementCard(document);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    tvMessage.setVisibility(View.VISIBLE);
                    tvMessage.setText(
                            "Unable to load announcements. Please try again."
                    );
                });
    }

    private void addAnnouncementCard(DocumentSnapshot document) {
        String title = document.getString("title");
        String message = document.getString("message");

        if (title == null || title.trim().isEmpty()) {
            title = "Announcement";
        }

        if (message == null) {
            message = "";
        }

        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.bottomMargin = dp(14);
        card.setLayoutParams(cardParams);
        card.setRadius(dp(16));
        card.setCardElevation(dp(2));
        card.setCardBackgroundColor(
                getColor(R.color.nexora_surface)
        );
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(getColor(R.color.nexora_border));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(18), dp(18), dp(18));

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(getColor(R.color.nexora_navy));
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvDate = new TextView(this);
        tvDate.setText(formatDate(document));
        tvDate.setTextColor(
                getColor(R.color.nexora_text_secondary)
        );
        tvDate.setTextSize(12);
        tvDate.setPadding(0, dp(6), 0, dp(12));

        TextView tvBody = new TextView(this);
        tvBody.setText(message);
        tvBody.setTextColor(
                getColor(R.color.nexora_text_primary)
        );
        tvBody.setTextSize(15);
        tvBody.setLineSpacing(dp(3), 1.0f);

        content.addView(tvTitle);
        content.addView(tvDate);
        content.addView(tvBody);

        card.addView(content);
        container.addView(card);
    }

    private String formatDate(DocumentSnapshot document) {
        if (document.getTimestamp("createdAt") == null) {
            return "Recently published";
        }

        Date date = document.getTimestamp("createdAt").toDate();

        SimpleDateFormat formatter = new SimpleDateFormat(
                "dd MMM yyyy, hh:mm a",
                Locale.getDefault()
        );

        return formatter.format(date);
    }

    private int dp(float value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }
}
