package com.example.nexora.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nexora.R;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MyAttendanceActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private TextView tvPercentage;
    private TextView tvSummary;
    private TextView tvMessage;
    private ProgressBar progress;
    private LinearLayout attendanceContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_attendance);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        tvPercentage = findViewById(R.id.tvAttendancePercentage);
        tvSummary = findViewById(R.id.tvAttendanceSummary);
        tvMessage = findViewById(R.id.tvMyAttendanceMessage);
        progress = findViewById(R.id.progressMyAttendance);
        attendanceContainer = findViewById(R.id.myAttendanceContainer);

        findViewById(R.id.btnAttendanceBack).setOnClickListener(v -> finish());

        loadAttendance();
    }

    private void loadAttendance() {
        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        progress.setVisibility(View.VISIBLE);
        tvMessage.setText("");

        // The query is restricted to the currently signed-in student's UID.
        db.collection("attendance")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    progress.setVisibility(View.GONE);
                    attendanceContainer.removeAllViews();

                    if (snapshot.isEmpty()) {
                        tvPercentage.setText("--%");
                        tvSummary.setText("No attendance records yet.");
                        tvMessage.setText("Your attendance records will appear here.");
                        return;
                    }

                    int total = 0;
                    int present = 0;
                    List<QueryDocumentSnapshot> records = new ArrayList<>();

                    for (QueryDocumentSnapshot document : snapshot) {
                        records.add(document);
                        total++;

                        String status = document.getString("status");
                        if (status != null
                                && status.trim().equalsIgnoreCase("present")) {
                            present++;
                        }
                    }

                    final int totalRecords = total;
                    final int presentRecords = present;
                    double percentage = totalRecords == 0
                            ? 0
                            : (presentRecords * 100.0) / totalRecords;

                    tvPercentage.setText(
                            String.format(Locale.getDefault(), "%.1f%%", percentage)
                    );

                    tvSummary.setText(
                            presentRecords + " present out of "
                                    + totalRecords + " recorded classes"
                    );

                    Collections.sort(records, new Comparator<QueryDocumentSnapshot>() {
                        @Override
                        public int compare(QueryDocumentSnapshot a,
                                           QueryDocumentSnapshot b) {
                            String dateA = a.getString("date");
                            String dateB = b.getString("date");

                            if (dateA == null) dateA = "";
                            if (dateB == null) dateB = "";

                            return dateB.compareTo(dateA);
                        }
                    });

                    for (QueryDocumentSnapshot document : records) {
                        addAttendanceCard(
                                document.getString("subjectName"),
                                document.getString("subjectCode"),
                                document.getString("date"),
                                document.getString("status")
                        );
                    }
                })
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    tvMessage.setText(
                            "Unable to load attendance. Please check your connection and permissions."
                    );
                    Toast.makeText(
                            this,
                            "Could not load attendance records.",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void addAttendanceCard(String subjectName, String subjectCode,
                                   String date, String status) {

        if (subjectName == null || subjectName.trim().isEmpty()) {
            subjectName = "Subject";
        }

        if (subjectCode == null) subjectCode = "";
        if (date == null || date.trim().isEmpty()) date = "Date unavailable";
        if (status == null || status.trim().isEmpty()) status = "Not specified";

        MaterialCardView card = new MaterialCardView(this);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.bottomMargin = dp(12);

        card.setLayoutParams(cardParams);
        card.setRadius(dp(14));
        card.setCardElevation(dp(1));
        card.setCardBackgroundColor(Color.WHITE);
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(Color.rgb(214, 220, 231));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(15), dp(16), dp(15));

        TextView title = new TextView(this);
        title.setText(subjectName);
        title.setTextColor(Color.rgb(23, 32, 51));
        title.setTextSize(16);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        content.addView(title);

        if (!subjectCode.trim().isEmpty()) {
            TextView code = new TextView(this);
            code.setText("Subject code: " + subjectCode);
            code.setTextColor(Color.rgb(104, 115, 134));
            code.setTextSize(12);
            code.setPadding(0, dp(4), 0, 0);
            content.addView(code);
        }

        TextView dateView = new TextView(this);
        dateView.setText("Date: " + date);
        dateView.setTextColor(Color.rgb(104, 115, 134));
        dateView.setTextSize(13);
        dateView.setPadding(0, dp(8), 0, 0);
        content.addView(dateView);

        TextView statusView = new TextView(this);
        statusView.setText("Status: " + status);
        statusView.setTextSize(14);
        statusView.setTypeface(null, android.graphics.Typeface.BOLD);
        statusView.setPadding(0, dp(8), 0, 0);

        if (status.equalsIgnoreCase("present")) {
            statusView.setTextColor(Color.rgb(22, 128, 76));
        } else if (status.equalsIgnoreCase("absent")) {
            statusView.setTextColor(Color.rgb(190, 45, 45));
        } else {
            statusView.setTextColor(Color.rgb(104, 115, 134));
        }

        content.addView(statusView);
        card.addView(content);
        attendanceContainer.addView(card);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

