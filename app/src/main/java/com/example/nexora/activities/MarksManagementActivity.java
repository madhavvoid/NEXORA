package com.example.nexora.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nexora.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MarksManagementActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    private Spinner spinnerStudent, spinnerSubject, spinnerExamType;
    private TextInputEditText etMarksObtained, etMaximumMarks;
    private MaterialButton btnSaveMarks;
    private ProgressBar progressMarks;
    private TextView tvMarksMessage;
    private LinearLayout marksContainer;

    private final List<String> studentUids = new ArrayList<>();
    private final List<String> studentNames = new ArrayList<>();
    private final List<String> studentIds = new ArrayList<>();
    private final List<String> studentLabels = new ArrayList<>();

    private final List<String> subjectIds = new ArrayList<>();
    private final List<String> subjectNames = new ArrayList<>();
    private final List<String> subjectCodes = new ArrayList<>();
    private final List<String> subjectLabels = new ArrayList<>();

    private boolean studentsLoaded = false;
    private boolean subjectsLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_marks_management);

        db = FirebaseFirestore.getInstance();

        spinnerStudent = findViewById(R.id.spinnerMarksStudent);
        spinnerSubject = findViewById(R.id.spinnerMarksSubject);
        spinnerExamType = findViewById(R.id.spinnerMarksExamType);

        etMarksObtained = findViewById(R.id.etMarksObtained);
        etMaximumMarks = findViewById(R.id.etMaximumMarks);

        btnSaveMarks = findViewById(R.id.btnSaveMarks);
        progressMarks = findViewById(R.id.progressMarks);
        tvMarksMessage = findViewById(R.id.tvMarksMessage);
        marksContainer = findViewById(R.id.marksContainer);

        MaterialButton btnBack = findViewById(R.id.btnMarksBack);
        btnBack.setOnClickListener(v -> finish());

        String[] examTypes = {
                "Internal Assessment",
                "Mid-Term Examination",
                "Final Examination",
                "Practical Examination"
        };

        ArrayAdapter<String> examAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                examTypes
        );
        examAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );
        spinnerExamType.setAdapter(examAdapter);

        btnSaveMarks.setOnClickListener(v -> saveMarks());

        btnSaveMarks.setEnabled(false);

        loadStudents();
        loadSubjects();
        loadMarksRecords();
    }

    private void showLoading(boolean loading) {
        progressMarks.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSaveMarks.setEnabled(!loading
                && studentsLoaded
                && subjectsLoaded
                && !studentUids.isEmpty()
                && !subjectIds.isEmpty());
    }

    private void loadStudents() {
        db.collection("users")
                .whereEqualTo("role", "student")
                .whereEqualTo("approvalStatus", "approved")
                .get()
                .addOnSuccessListener(snapshot -> {
                    studentUids.clear();
                    studentNames.clear();
                    studentIds.clear();
                    studentLabels.clear();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String uid = doc.getId();
                        String name = doc.getString("name");
                        String studentId = doc.getString("studentId");

                        if (name == null || name.trim().isEmpty()) {
                            name = "Unnamed student";
                        }
                        if (studentId == null) {
                            studentId = "";
                        }

                        studentUids.add(uid);
                        studentNames.add(name);
                        studentIds.add(studentId);

                        studentLabels.add(studentId.isEmpty()
                                ? name
                                : name + " (" + studentId + ")");
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
                    updateSaveButton();

                    if (studentLabels.isEmpty()) {
                        tvMarksMessage.setText(
                                "No approved students found. Approve a student first."
                        );
                    }
                })
                .addOnFailureListener(e -> {
                    tvMarksMessage.setText(
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
                    subjectIds.clear();
                    subjectNames.clear();
                    subjectCodes.clear();
                    subjectLabels.clear();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String name = doc.getString("subjectName");
                        String code = doc.getString("subjectCode");

                        if (name == null) {
                            name = "Unnamed subject";
                        }
                        if (code == null) {
                            code = "";
                        }

                        subjectIds.add(doc.getId());
                        subjectNames.add(name);
                        subjectCodes.add(code);

                        subjectLabels.add(code.isEmpty()
                                ? name
                                : name + " (" + code + ")");
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
                    updateSaveButton();

                    if (subjectLabels.isEmpty()) {
                        tvMarksMessage.setText(
                                "No subjects found. Add a subject first."
                        );
                    }
                })
                .addOnFailureListener(e ->
                        tvMarksMessage.setText(
                                "Could not load subjects: " + e.getMessage()
                        )
                );
    }

    private void updateSaveButton() {
        btnSaveMarks.setEnabled(
                studentsLoaded
                        && subjectsLoaded
                        && !studentUids.isEmpty()
                        && !subjectIds.isEmpty()
        );
    }

    private void saveMarks() {
        if (studentUids.isEmpty() || subjectIds.isEmpty()) {
            Toast.makeText(
                    this,
                    "An approved student and a subject are required",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        String obtainedText = etMarksObtained.getText() == null
                ? ""
                : etMarksObtained.getText().toString().trim();

        String maximumText = etMaximumMarks.getText() == null
                ? ""
                : etMaximumMarks.getText().toString().trim();

        if (obtainedText.isEmpty()) {
            etMarksObtained.setError("Enter marks obtained");
            return;
        }

        if (maximumText.isEmpty()) {
            etMaximumMarks.setError("Enter maximum marks");
            return;
        }

        double obtained;
        double maximum;

        try {
            obtained = Double.parseDouble(obtainedText);
            maximum = Double.parseDouble(maximumText);
        } catch (NumberFormatException e) {
            Toast.makeText(
                    this,
                    "Enter valid numeric marks",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (Double.isNaN(obtained) || Double.isInfinite(obtained)
                || Double.isNaN(maximum) || Double.isInfinite(maximum)
                || maximum <= 0 || obtained < 0 || obtained > maximum) {
            Toast.makeText(
                    this,
                    "Marks must be between 0 and the maximum marks",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        int studentPosition = spinnerStudent.getSelectedItemPosition();
        int subjectPosition = spinnerSubject.getSelectedItemPosition();

        if (studentPosition < 0 || subjectPosition < 0) {
            Toast.makeText(
                    this,
                    "Select a student and subject",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String uid = studentUids.get(studentPosition);
        String studentName = studentNames.get(studentPosition);
        String studentId = studentIds.get(studentPosition);

        String subjectId = subjectIds.get(subjectPosition);
        String subjectName = subjectNames.get(subjectPosition);
        String subjectCode = subjectCodes.get(subjectPosition);
        String examType = spinnerExamType.getSelectedItem().toString();

        // Stable ID to avoid duplicate records for the same exam.
        String recordId = uid + "_" + subjectId + "_"
                + examType.replaceAll("[^a-zA-Z0-9]", "");

        Map<String, Object> record = new HashMap<>();
        record.put("userId", uid);
        record.put("studentName", studentName);
        record.put("studentId", studentId);
        record.put("subjectId", subjectId);
        record.put("subjectName", subjectName);
        record.put("subjectCode", subjectCode);
        record.put("examType", examType);
        record.put("marksObtained", obtained);
        record.put("maximumMarks", maximum);
        record.put("percentage", (obtained / maximum) * 100);
        record.put("createdAt", FieldValue.serverTimestamp());

        showLoading(true);

        db.collection("marks")
                .document(recordId)
                .get()
                .addOnSuccessListener(existing -> {
                    if (existing.exists()) {
                        showLoading(false);
                        Toast.makeText(
                                this,
                                "Marks already exist for this student, subject and exam type",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    db.collection("marks")
                            .document(recordId)
                            .set(record)
                            .addOnSuccessListener(unused -> {
                                showLoading(false);

                                etMarksObtained.setText("");
                                etMaximumMarks.setText("");

                                tvMarksMessage.setText(
                                        "Marks saved successfully for "
                                                + studentName
                                );

                                Toast.makeText(
                                        this,
                                        "Marks saved successfully",
                                        Toast.LENGTH_SHORT
                                ).show();

                                loadMarksRecords();
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
                            "Could not check existing marks: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void loadMarksRecords() {
        marksContainer.removeAllViews();

        db.collection("marks")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        addRecordMessage("No marks records yet.");
                        return;
                    }

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String name = doc.getString("studentName");
                        String subject = doc.getString("subjectName");
                        String exam = doc.getString("examType");
                        Double obtained = doc.getDouble("marksObtained");
                        Double maximum = doc.getDouble("maximumMarks");

                        if (name == null) name = "Unknown student";
                        if (subject == null) subject = "Unknown subject";
                        if (exam == null) exam = "Unknown exam";
                        if (obtained == null) obtained = 0.0;
                        if (maximum == null) maximum = 0.0;

                        TextView recordView = new TextView(this);
                        recordView.setText(String.format(
                                Locale.US,
                                "%s\n%s — %s\nMarks: %.2f / %.2f",
                                name, subject, exam, obtained, maximum
                        ));
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

                        LinearLayout.LayoutParams params =
                                new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                );
                        params.bottomMargin = 16;
                        marksContainer.addView(recordView, params);
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
        marksContainer.addView(textView);
    }
}

