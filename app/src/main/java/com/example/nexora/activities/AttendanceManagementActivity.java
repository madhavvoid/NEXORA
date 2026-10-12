package com.example.nexora.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nexora.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AttendanceManagementActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    private Spinner spinnerStudent;
    private Spinner spinnerSubject;
    private Spinner spinnerStatus;

    private TextInputEditText etDate;
    private MaterialButton btnSaveAttendance;
    private ProgressBar progressAttendance;
    private TextView tvAttendanceMessage;
    private android.widget.LinearLayout attendanceContainer;

    private final List<String> studentLabels = new ArrayList<>();
    private final List<String> studentUids = new ArrayList<>();
    private final List<String> studentIds = new ArrayList<>();
    private final List<String> studentNames = new ArrayList<>();

    private final List<String> subjectLabels = new ArrayList<>();
    private final List<String> subjectDocIds = new ArrayList<>();
    private final List<String> subjectNames = new ArrayList<>();
    private final List<String> subjectCodes = new ArrayList<>();

    private boolean studentsLoaded = false;
    private boolean subjectsLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_management);

        db = FirebaseFirestore.getInstance();

        spinnerStudent = findViewById(R.id.spinnerAttendanceStudent);
        spinnerSubject = findViewById(R.id.spinnerAttendanceSubject);
        spinnerStatus = findViewById(R.id.spinnerAttendanceStatus);

        etDate = findViewById(R.id.etAttendanceDate);
        btnSaveAttendance = findViewById(R.id.btnSaveAttendance);
        progressAttendance = findViewById(R.id.progressAttendance);
        tvAttendanceMessage = findViewById(R.id.tvAttendanceMessage);
        attendanceContainer = findViewById(R.id.attendanceContainer);

        MaterialButton btnBack = findViewById(R.id.btnAttendanceBack);
        btnBack.setOnClickListener(v -> finish());

        // Set today's date in yyyy-MM-dd format.
        etDate.setText(
                new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date())
        );

        // Attendance status options.
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Present", "Absent"}
        );
        statusAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );
        spinnerStatus.setAdapter(statusAdapter);

        btnSaveAttendance.setOnClickListener(v -> saveAttendance());

        loadStudents();
        loadSubjects();
        loadAttendanceRecords();
    }

    private void showLoading(boolean loading) {
        progressAttendance.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSaveAttendance.setEnabled(!loading);
    }

    private void loadStudents() {
        db.collection("users")
                .whereEqualTo("role", "student")
                .whereEqualTo("approvalStatus", "approved")
                .get()
                .addOnSuccessListener(snapshot -> {
                    studentLabels.clear();
                    studentUids.clear();
                    studentIds.clear();
                    studentNames.clear();

                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        String uid = document.getId();
                        String name = document.getString("name");
                        String studentId = document.getString("studentId");

                        if (name == null || name.trim().isEmpty()) {
                            name = "Unnamed student";
                        }

                        if (studentId == null) {
                            studentId = "";
                        }

                        studentUids.add(uid);
                        studentNames.add(name);
                        studentIds.add(studentId);
                        studentLabels.add(
                                studentId.isEmpty()
                                        ? name
                                        : name + " (" + studentId + ")"
                        );
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_item,
                            studentLabels
                    );
                    adapter.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item
                    );
                    spinnerStudent.setAdapter(adapter);

                    studentsLoaded = true;
                    updateFormAvailability();

                    if (studentLabels.isEmpty()) {
                        tvAttendanceMessage.setText(
                                "No approved students found. Approve a student first."
                        );
                    }
                })
                .addOnFailureListener(e -> {
                    tvAttendanceMessage.setText(
                            "Could not load students: " + e.getMessage()
                    );
                    Toast.makeText(
                            this,
                            "Failed to load students",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void loadSubjects() {
        db.collection("subjects")
                .get()
                .addOnSuccessListener(snapshot -> {
                    subjectLabels.clear();
                    subjectDocIds.clear();
                    subjectNames.clear();
                    subjectCodes.clear();

                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        String name = document.getString("subjectName");
                        String code = document.getString("subjectCode");

                        if (name == null) {
                            name = "Unnamed subject";
                        }

                        if (code == null) {
                            code = "";
                        }

                        subjectDocIds.add(document.getId());
                        subjectNames.add(name);
                        subjectCodes.add(code);
                        subjectLabels.add(
                                code.isEmpty() ? name : name + " (" + code + ")"
                        );
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_item,
                            subjectLabels
                    );
                    adapter.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item
                    );
                    spinnerSubject.setAdapter(adapter);

                    subjectsLoaded = true;
                    updateFormAvailability();

                    if (subjectLabels.isEmpty()) {
                        tvAttendanceMessage.setText(
                                "No subjects found. Add a subject first."
                        );
                    }
                })
                .addOnFailureListener(e ->
                        tvAttendanceMessage.setText(
                                "Could not load subjects: " + e.getMessage()
                        )
                );
    }

    private void updateFormAvailability() {
        btnSaveAttendance.setEnabled(
                studentsLoaded
                        && subjectsLoaded
                        && !studentUids.isEmpty()
                        && !subjectDocIds.isEmpty()
        );
    }

    private void saveAttendance() {
        if (studentUids.isEmpty()) {
            Toast.makeText(
                    this, "No approved students available",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (subjectDocIds.isEmpty()) {
            Toast.makeText(
                    this, "Please add a subject first",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String date = etDate.getText() == null
                ? ""
                : etDate.getText().toString().trim();

        SimpleDateFormat dateFormat =
                new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        dateFormat.setLenient(false);

        try {
            dateFormat.parse(date);
        } catch (Exception e) {
            etDate.setError("Use date format yyyy-MM-dd");
            return;
        }

        int studentPosition = spinnerStudent.getSelectedItemPosition();
        int subjectPosition = spinnerSubject.getSelectedItemPosition();

        if (studentPosition < 0 || subjectPosition < 0) {
            Toast.makeText(
                    this, "Select a student and subject",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String uid = studentUids.get(studentPosition);
        String studentName = studentNames.get(studentPosition);
        String studentId = studentIds.get(studentPosition);

        String subjectId = subjectDocIds.get(subjectPosition);
        String subjectName = subjectNames.get(subjectPosition);
        String subjectCode = subjectCodes.get(subjectPosition);

        String status = spinnerStatus.getSelectedItem().toString();

        // One document per student, subject and date.
        String recordId = uid + "_" + subjectId + "_" + date;

        Map<String, Object> record = new HashMap<>();
        record.put("userId", uid);
        record.put("studentName", studentName);
        record.put("studentId", studentId);
        record.put("subjectId", subjectId);
        record.put("subjectName", subjectName);
        record.put("subjectCode", subjectCode);
        record.put("date", date);
        record.put("status", status);
        record.put("createdAt", FieldValue.serverTimestamp());

        showLoading(true);

        db.collection("attendance")
                .document(recordId)
                .get()
                .addOnSuccessListener(existing -> {
                    if (existing.exists()) {
                        showLoading(false);
                        Toast.makeText(
                                this,
                                "Attendance already exists for this student, subject and date",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    db.collection("attendance")
                            .document(recordId)
                            .set(record)
                            .addOnSuccessListener(unused -> {
                                showLoading(false);
                                Toast.makeText(
                                        this,
                                        "Attendance saved successfully",
                                        Toast.LENGTH_SHORT
                                ).show();

                                tvAttendanceMessage.setText(
                                        "Attendance saved for " + studentName
                                );

                                loadAttendanceRecords();
                            })
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                Toast.makeText(
                                        this,
                                        "Save failed: " + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(
                            this,
                            "Could not check existing attendance: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void loadAttendanceRecords() {
        attendanceContainer.removeAllViews();

        db.collection("attendance")
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        addRecordMessage("No attendance records yet.");
                        return;
                    }

                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        String name = document.getString("studentName");
                        String subject = document.getString("subjectName");
                        String code = document.getString("subjectCode");
                        String date = document.getString("date");
                        String status = document.getString("status");

                        if (name == null) name = "Unknown student";
                        if (subject == null) subject = "Unknown subject";
                        if (code == null) code = "";
                        if (date == null) date = "";
                        if (status == null) status = "";

                        TextView recordView = new TextView(this);
                        recordView.setText(
                                name + "\n"
                                        + subject
                                        + (code.isEmpty() ? "" : " (" + code + ")")
                                        + "\n" + date + "  •  " + status
                        );
                        recordView.setTextSize(14);
                        recordView.setTextColor(
                                getResources().getColor(
                                        R.color.nexora_text_primary,
                                        getTheme()
                                )
                        );
                        recordView.setPadding(24, 20, 24, 20);

                        android.graphics.drawable.GradientDrawable background =
                                new android.graphics.drawable.GradientDrawable();
                        background.setColor(
                                getResources().getColor(
                                        R.color.nexora_surface,
                                        getTheme()
                                )
                        );
                        background.setCornerRadius(20);
                        background.setStroke(
                                1,
                                getResources().getColor(
                                        R.color.nexora_border,
                                        getTheme()
                                )
                        );
                        recordView.setBackground(background);

                        android.widget.LinearLayout.LayoutParams params =
                                new android.widget.LinearLayout.LayoutParams(
                                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                                );
                        params.bottomMargin = 16;
                        attendanceContainer.addView(recordView, params);
                    }
                })
                .addOnFailureListener(e ->
                        addRecordMessage(
                                "Could not load records: " + e.getMessage()
                        )
                );
    }

    private void addRecordMessage(String message) {
        TextView textView = new TextView(this);
        textView.setText(message);
        textView.setTextSize(14);
        textView.setPadding(8, 16, 8, 16);
        attendanceContainer.addView(textView);
    }
}

