package com.example.nexora.activities;

import android.graphics.Color;
import android.graphics.Typeface;
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

public class MyResultsActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private TextView tvAverage;
    private TextView tvSummary;
    private TextView tvMessage;
    private ProgressBar progress;
    private LinearLayout resultsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_results);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvAverage = findViewById(R.id.tvResultsAverage);
        tvSummary = findViewById(R.id.tvResultsSummary);
        tvMessage = findViewById(R.id.tvMyResultsMessage);
        progress = findViewById(R.id.progressMyResults);
        resultsContainer = findViewById(R.id.myResultsContainer);

        findViewById(R.id.btnResultsBack).setOnClickListener(v -> finish());

        loadResults();
    }

    private void loadResults() {
        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        progress.setVisibility(View.VISIBLE);
        tvMessage.setText("");

        // Request only results belonging to the signed-in student.
        db.collection("marks")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    progress.setVisibility(View.GONE);
                    resultsContainer.removeAllViews();

                    if (snapshot.isEmpty()) {
                        tvAverage.setText("--%");
                        tvSummary.setText("No results recorded yet.");
                        tvMessage.setText(
                                "Your exam results will appear here when published."
                        );
                        return;
                    }

                    List<QueryDocumentSnapshot> records = new ArrayList<>();

                    double totalObtained = 0;
                    double totalMaximum = 0;
                    int validRecords = 0;

                    for (QueryDocumentSnapshot document : snapshot) {
                        records.add(document);

                        Double obtained =
                                document.getDouble("marksObtained");
                        Double maximum =
                                document.getDouble("maximumMarks");

                        if (obtained != null && maximum != null
                                && maximum > 0) {
                            totalObtained += obtained;
                            totalMaximum += maximum;
                            validRecords++;
                        }
                    }

                    if (totalMaximum > 0) {
                        double average =
                                (totalObtained / totalMaximum) * 100.0;

                        tvAverage.setText(String.format(
                                Locale.getDefault(), "%.1f%%", average
                        ));
                    } else {
                        tvAverage.setText("--%");
                    }

                    tvSummary.setText(
                            records.size() + " result record(s) available"
                    );

                    Collections.sort(
                            records,
                            new Comparator<QueryDocumentSnapshot>() {
                                @Override
                                public int compare(
                                        QueryDocumentSnapshot a,
                                        QueryDocumentSnapshot b) {
                                    String typeA = a.getString("examType");
                                    String typeB = b.getString("examType");

                                    if (typeA == null) typeA = "";
                                    if (typeB == null) typeB = "";

                                    return typeA.compareToIgnoreCase(typeB);
                                }
                            }
                    );

                    for (QueryDocumentSnapshot document : records) {
                        addResultCard(
                                document.getString("subjectName"),
                                document.getString("subjectCode"),
                                document.getString("examType"),
                                document.get("marksObtained"),
                                document.get("maximumMarks"),
                                document.get("percentage")
                        );
                    }
                })
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    tvMessage.setText(
                            "Unable to load results. Check your connection and permissions."
                    );

                    Toast.makeText(
                            this,
                            "Could not load results.",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void addResultCard(
            String subjectName,
            String subjectCode,
            String examType,
            Object obtainedValue,
            Object maximumValue,
            Object percentageValue) {

        if (subjectName == null || subjectName.trim().isEmpty()) {
            subjectName = "Subject";
        }

        if (subjectCode == null) subjectCode = "";
        if (examType == null || examType.trim().isEmpty()) {
            examType = "Exam";
        }

        Double obtained = toDouble(obtainedValue);
        Double maximum = toDouble(maximumValue);
        Double percentage = toDouble(percentageValue);

        if (percentage == null && obtained != null
                && maximum != null && maximum > 0) {
            percentage = (obtained / maximum) * 100.0;
        }

        MaterialCardView card = new MaterialCardView(this);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
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
        content.setPadding(dp(16), dp(16), dp(16), dp(16));

        TextView title = new TextView(this);
        title.setText(subjectName);
        title.setTextColor(Color.rgb(23, 32, 51));
        title.setTextSize(17);
        title.setTypeface(null, Typeface.BOLD);
        content.addView(title);

        if (!subjectCode.trim().isEmpty()) {
            TextView code = new TextView(this);
            code.setText("Subject code: " + subjectCode);
            code.setTextColor(Color.rgb(104, 115, 134));
            code.setTextSize(12);
            code.setPadding(0, dp(4), 0, 0);
            content.addView(code);
        }

        TextView exam = new TextView(this);
        exam.setText("Exam: " + examType);
        exam.setTextColor(Color.rgb(104, 115, 134));
        exam.setTextSize(14);
        exam.setPadding(0, dp(10), 0, 0);
        content.addView(exam);

        TextView marks = new TextView(this);
        if (obtained != null && maximum != null) {
            marks.setText("Marks: " + formatNumber(obtained)
                    + " / " + formatNumber(maximum));
        } else {
            marks.setText("Marks: Not available");
        }
        marks.setTextColor(Color.rgb(23, 32, 51));
        marks.setTextSize(15);
        marks.setTypeface(null, Typeface.BOLD);
        marks.setPadding(0, dp(8), 0, 0);
        content.addView(marks);

        TextView percentageView = new TextView(this);
        if (percentage != null) {
            percentageView.setText(String.format(
                    Locale.getDefault(), "Percentage: %.1f%%", percentage
            ));

            if (percentage >= 75) {
                percentageView.setTextColor(Color.rgb(22, 128, 76));
            } else if (percentage >= 40) {
                percentageView.setTextColor(Color.rgb(180, 110, 15));
            } else {
                percentageView.setTextColor(Color.rgb(190, 45, 45));
            }
        } else {
            percentageView.setText("Percentage: Not available");
            percentageView.setTextColor(Color.rgb(104, 115, 134));
        }

        percentageView.setTextSize(14);
        percentageView.setTypeface(null, Typeface.BOLD);
        percentageView.setPadding(0, dp(8), 0, 0);
        content.addView(percentageView);

        card.addView(content);
        resultsContainer.addView(card);
    }

    private Double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    private String formatNumber(double value) {
        if (value == Math.floor(value)) {
            return String.format(Locale.getDefault(), "%.0f", value);
        }
        return String.format(Locale.getDefault(), "%.2f", value);
    }

    private int dp(int value) {
        return Math.round(
                value * getResources().getDisplayMetrics().density
        );
    }
}
