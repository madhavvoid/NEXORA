
package com.example.nexora.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.graphics.Typeface;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nexora.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class StudentSubjectsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ProgressBar progressBar;
    private TextView tvMessage;
    private TextView tvCount;
    private TextView tvCourse;
    private LinearLayout container;

    private String studentCourse = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_subjects);

        progressBar = findViewById(R.id.progressStudentSubjects);
        tvMessage = findViewById(R.id.tvStudentSubjectsMessage);
        tvCount = findViewById(R.id.tvStudentSubjectsCount);
        tvCourse = findViewById(R.id.tvStudentSubjectsCourse);
        container = findViewById(R.id.studentSubjectsContainer);

        findViewById(R.id.btnStudentSubjectsBack)
                .setOnClickListener(v -> finish());

        db = FirebaseFirestore.getInstance();

        loadStudentCourse();
    }

    private void loadStudentCourse() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        showMessage("Student profile not found.");
                        return;
                    }

                    studentCourse = document.getString("course");

                    if (studentCourse == null) {
                        studentCourse = "";
                    }

                    if (!studentCourse.trim().isEmpty()) {
                        tvCourse.setText("Course: " + studentCourse);
                    } else {
                        tvCourse.setText("Your academic subjects");
                    }

                    loadSubjects();
                })
                .addOnFailureListener(e ->
                        showMessage("Unable to load your profile. Please try again."));
    }

    private void loadSubjects() {
        progressBar.setVisibility(View.VISIBLE);
        tvMessage.setVisibility(View.VISIBLE);
        tvMessage.setText("Loading subjects...");
        container.removeAllViews();

        db.collection("subjects")
                .orderBy("subjectName", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    progressBar.setVisibility(View.GONE);
                    container.removeAllViews();

                    List<DocumentSnapshot> matchingSubjects = new ArrayList<>();

                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        String subjectCourse = document.getString("course");

                        // Show only subjects matching the student's course.
                        // If the student's course is not specified, show all subjects.
                        if (studentCourse.trim().isEmpty()
                                || studentCourse.equalsIgnoreCase(
                                subjectCourse == null ? "" : subjectCourse.trim())) {
                            matchingSubjects.add(document);
                        }
                    }

                    int count = matchingSubjects.size();
                    tvCount.setText(count + (count == 1
                            ? " subject available"
                            : " subjects available"));

                    if (count == 0) {
                        showMessage("No subjects found for your course yet.");
                        return;
                    }

                    tvMessage.setVisibility(View.GONE);

                    for (DocumentSnapshot document : matchingSubjects) {
                        addSubjectCard(document);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    tvCount.setText("Subjects unavailable");
                    showMessage("Unable to load subjects. Check your connection and try again.");
                });
    }

    private void addSubjectCard(DocumentSnapshot document) {
        String name = document.getString("subjectName");
        String code = document.getString("subjectCode");
        String course = document.getString("course");

        Long semesterValue = document.getLong("semester");
        String semester = semesterValue == null
                ? "Not specified"
                : "Semester " + semesterValue;

        if (name == null || name.trim().isEmpty()) {
            name = "Unnamed Subject";
        }

        if (code == null || code.trim().isEmpty()) {
            code = "Code not available";
        }

        if (course == null || course.trim().isEmpty()) {
            course = "Course not specified";
        }

        MaterialCardView card = new MaterialCardView(this);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.bottomMargin = dp(14);
        card.setLayoutParams(cardParams);
        card.setRadius(dp(14));
        card.setCardElevation(dp(2));
        card.setCardBackgroundColor(getColor(R.color.nexora_surface));
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(getColor(R.color.nexora_border));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(18), dp(18), dp(18));

        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextColor(getColor(R.color.nexora_navy));
        tvName.setTextSize(18);
        tvName.setTypeface(null, Typeface.BOLD);

        TextView tvCode = new TextView(this);
        tvCode.setText("Subject Code: " + code);
        tvCode.setTextColor(getColor(R.color.nexora_blue));
        tvCode.setTextSize(14);
        tvCode.setPadding(0, dp(8), 0, dp(5));

        TextView tvSemester = new TextView(this);
        tvSemester.setText(semester);
        tvSemester.setTextColor(getColor(R.color.nexora_text_primary));
        tvSemester.setTextSize(14);
        tvSemester.setPadding(0, dp(4), 0, dp(4));

        TextView tvCourseInfo = new TextView(this);
        tvCourseInfo.setText(course);
        tvCourseInfo.setTextColor(getColor(R.color.nexora_text_secondary));
        tvCourseInfo.setTextSize(13);
        tvCourseInfo.setPadding(0, dp(4), 0, 0);

        content.addView(tvName);
        content.addView(tvCode);
        content.addView(tvSemester);
        content.addView(tvCourseInfo);

        card.addView(content);
        container.addView(card);
    }

    private void showMessage(String message) {
        progressBar.setVisibility(View.GONE);
        tvMessage.setVisibility(View.VISIBLE);
        tvMessage.setText(message);
    }

    private int dp(float value) {
        return (int) (value
                * getResources().getDisplayMetrics().density + 0.5f);
    }
}
