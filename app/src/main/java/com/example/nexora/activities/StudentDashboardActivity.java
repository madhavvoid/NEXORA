package com.example.nexora.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nexora.R;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Locale;

public class StudentDashboardActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private TextView tvWelcome;
    private TextView tvStudentDetails;
    private TextView tvLogout;

    private TextView tvAttendanceStat;
    private TextView tvResultsStat;
    private TextView tvSubjectsStat;
    private TextView tvAnnouncementsStat;

    private MaterialCardView cardMyAttendance;
    private MaterialCardView cardMyResults;
    private MaterialCardView cardMySubjects;
    private MaterialCardView cardAnnouncements;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvWelcome = findViewById(R.id.tvWelcome);
        tvStudentDetails = findViewById(R.id.tvStudentDetails);
        tvLogout = findViewById(R.id.tvLogout);

        tvAttendanceStat = findViewById(R.id.tvAttendanceStat);
        tvResultsStat = findViewById(R.id.tvResultsStat);
        tvSubjectsStat = findViewById(R.id.tvSubjectsStat);
        tvAnnouncementsStat = findViewById(R.id.tvAnnouncementsStat);

        cardMyAttendance = findViewById(R.id.cardMyAttendance);
        cardMyResults = findViewById(R.id.cardMyResults);
        cardMySubjects = findViewById(R.id.cardMySubjects);
        cardAnnouncements = findViewById(R.id.cardAnnouncements);

        tvAttendanceStat.setText("Attendance: Loading...");
        tvResultsStat.setText("Average: Loading...");
        tvSubjectsStat.setText("Subjects: Loading...");
        tvAnnouncementsStat.setText("Announcements not available yet");

        loadStudentProfile();
        loadAttendanceStatistic();
        loadResultsStatistic();
        loadSubjectsStatistic();

        cardMyAttendance.setOnClickListener(v -> {
            startActivity(new Intent(
                    StudentDashboardActivity.this,
                    MyAttendanceActivity.class
            ));
        });

        cardMyResults.setOnClickListener(v -> {
            startActivity(new Intent(
                    StudentDashboardActivity.this,
                    MyResultsActivity.class
            ));
        });


        cardMySubjects.setOnClickListener(v -> {
            Intent intent = new Intent(
                    StudentDashboardActivity.this,
                    StudentSubjectsActivity.class
            );
            startActivity(intent);
        });



        cardAnnouncements.setOnClickListener(v -> {
            Intent intent = new Intent(
                    StudentDashboardActivity.this,
                    StudentAnnouncementsActivity.class
            );
            startActivity(intent);
        });


        tvLogout.setOnClickListener(v -> {
            auth.signOut();
            goToLogin();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (auth != null && auth.getCurrentUser() != null) {
            loadAttendanceStatistic();
            loadResultsStatistic();
            loadSubjectsStatistic();
        }
    }

    private void loadStudentProfile() {
        if (auth.getCurrentUser() == null) {
            goToLogin();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        tvWelcome.setText("Welcome to Nexora");
                        tvStudentDetails.setText(
                                "Your student profile could not be found."
                        );
                        return;
                    }

                    String name = document.getString("name");
                    String studentId = document.getString("studentId");
                    String course = document.getString("course");

                    if (name == null || name.trim().isEmpty()) {
                        name = "Student";
                    }

                    tvWelcome.setText("Welcome, " + name);

                    StringBuilder details = new StringBuilder();

                    if (studentId != null && !studentId.trim().isEmpty()) {
                        details.append("Student ID: ").append(studentId);
                    }

                    if (course != null && !course.trim().isEmpty()) {
                        if (details.length() > 0) {
                            details.append("\n");
                        }
                        details.append("Course: ").append(course);
                    }

                    if (details.length() == 0) {
                        details.append("Your academic overview");
                    }

                    tvStudentDetails.setText(details.toString());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Unable to load student profile.",
                                Toast.LENGTH_SHORT
                        ).show()
                );
    }

    private void loadAttendanceStatistic() {
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();

        db.collection("attendance")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    int total = snapshot.size();
                    int present = 0;

                    for (com.google.firebase.firestore.DocumentSnapshot doc
                            : snapshot.getDocuments()) {

                        String status = doc.getString("status");

                        if (status != null
                                && status.trim().equalsIgnoreCase("present")) {
                            present++;
                        }
                    }

                    if (total == 0) {
                        tvAttendanceStat.setText("Attendance: No records");
                    } else {
                        double percentage = present * 100.0 / total;

                        tvAttendanceStat.setText(String.format(
                                Locale.getDefault(),
                                "Attendance: %.1f%%",
                                percentage
                        ));
                    }
                })
                .addOnFailureListener(e ->
                        tvAttendanceStat.setText("Attendance: Unavailable")
                );
    }

    private void loadResultsStatistic() {
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();

        db.collection("marks")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    double obtainedTotal = 0;
                    double maximumTotal = 0;

                    for (com.google.firebase.firestore.DocumentSnapshot doc
                            : snapshot.getDocuments()) {

                        Double obtained = doc.getDouble("marksObtained");
                        Double maximum = doc.getDouble("maximumMarks");

                        if (obtained != null && maximum != null && maximum > 0) {
                            obtainedTotal += obtained;
                            maximumTotal += maximum;
                        }
                    }

                    if (maximumTotal == 0) {
                        tvResultsStat.setText("Average: No results");
                    } else {
                        double average =
                                obtainedTotal * 100.0 / maximumTotal;

                        tvResultsStat.setText(String.format(
                                Locale.getDefault(),
                                "Average: %.1f%%",
                                average
                        ));
                    }
                })
                .addOnFailureListener(e ->
                        tvResultsStat.setText("Average: Unavailable")
                );
    }

    private void loadSubjectsStatistic() {
        db.collection("subjects")
                .get()
                .addOnSuccessListener(snapshot ->
                        tvSubjectsStat.setText(
                                "Subjects: " + snapshot.size()
                        )
                )
                .addOnFailureListener(e ->
                        tvSubjectsStat.setText("Subjects: Unavailable")
                );
    }

    private void goToLogin() {
        Intent intent = new Intent(
                StudentDashboardActivity.this,
                LoginActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}
