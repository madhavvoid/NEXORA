
package com.example.nexora.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nexora.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SubjectManagementActivity extends AppCompatActivity {

    private TextInputEditText etSubjectName;
    private TextInputEditText etSubjectCode;
    private TextInputEditText etSubjectCourse;
    private TextInputEditText etSubjectSemester;

    private LinearLayout subjectContainer;
    private ProgressBar progressSubjects;
    private TextView tvSubjectStatus;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subject_management);

        etSubjectName = findViewById(R.id.etSubjectName);
        etSubjectCode = findViewById(R.id.etSubjectCode);
        etSubjectCourse = findViewById(R.id.etSubjectCourse);
        etSubjectSemester = findViewById(R.id.etSubjectSemester);

        subjectContainer = findViewById(R.id.subjectContainer);
        progressSubjects = findViewById(R.id.progressSubjects);
        tvSubjectStatus = findViewById(R.id.tvSubjectStatus);

        db = FirebaseFirestore.getInstance();

        findViewById(R.id.btnSubjectBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnAddSubject).setOnClickListener(v ->
                addSubject()
        );

        loadSubjects();
    }

    private void addSubject() {
        String name = getText(etSubjectName);
        String code = getText(etSubjectCode).toUpperCase();
        String course = getText(etSubjectCourse).toUpperCase();
        String semesterText = getText(etSubjectSemester);

        if (name.isEmpty() || code.isEmpty()
                || course.isEmpty() || semesterText.isEmpty()) {
            Toast.makeText(
                    this,
                    "Please fill in all subject fields.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        int semester;

        try {
            semester = Integer.parseInt(semesterText);

            if (semester < 1 || semester > 20) {
                Toast.makeText(
                        this,
                        "Enter a valid semester number.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(
                    this,
                    "Semester must be a number.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        setLoading(true);

        // Check for a duplicate subject code in the same course/semester.
        db.collection("subjects")
                .whereEqualTo("subjectCode", code)
                .whereEqualTo("course", course)
                .whereEqualTo("semester", semester)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        setLoading(false);
                        Toast.makeText(
                                this,
                                "This subject code already exists for this course and semester.",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    saveSubject(name, code, course, semester);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(
                            this,
                            "Could not check subject code. " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void saveSubject(
            String name,
            String code,
            String course,
            int semester
    ) {
        Map<String, Object> subject = new HashMap<>();

        subject.put("subjectName", name);
        subject.put("subjectCode", code);
        subject.put("course", course);
        subject.put("semester", semester);
        subject.put("createdAt", FieldValue.serverTimestamp());

        db.collection("subjects")
                .add(subject)
                .addOnSuccessListener(documentReference -> {
                    setLoading(false);

                    etSubjectName.setText("");
                    etSubjectCode.setText("");
                    etSubjectCourse.setText("");
                    etSubjectSemester.setText("");

                    Toast.makeText(
                            this,
                            "Subject added successfully!",
                            Toast.LENGTH_SHORT
                    ).show();

                    loadSubjects();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);

                    Toast.makeText(
                            this,
                            "Could not save subject. " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void loadSubjects() {
        progressSubjects.setVisibility(View.VISIBLE);
        tvSubjectStatus.setVisibility(View.GONE);
        subjectContainer.removeAllViews();

        db.collection("subjects")
                .addSnapshotListener((snapshot, e) -> {
                    progressSubjects.setVisibility(View.GONE);
                    subjectContainer.removeAllViews();

                    if (e != null) {
                        tvSubjectStatus.setText(
                                "Could not load subjects. Check permissions."
                        );
                        tvSubjectStatus.setVisibility(View.VISIBLE);
                        Toast.makeText(
                                this,
                                "Loading failed: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    if (snapshot == null || snapshot.isEmpty()) {
                        tvSubjectStatus.setText(
                                "No subjects added yet. Use the form above to add one."
                        );
                        tvSubjectStatus.setVisibility(View.VISIBLE);
                        return;
                    }

                    tvSubjectStatus.setVisibility(View.GONE);

                    for (DocumentSnapshot document : snapshot) {
                        addSubjectCard(document);
                    }
                });
    }

    private void addSubjectCard(DocumentSnapshot document) {
        String name = getStringValue(document, "subjectName");
        String code = getStringValue(document, "subjectCode");
        String course = getStringValue(document, "course");
        Long semesterValue = document.getLong("semester");

        String semester = semesterValue == null
                ? "Not provided"
                : String.valueOf(semesterValue);

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
        card.setCardBackgroundColor(
                getColor(R.color.nexora_surface)
        );
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(
                getColor(R.color.nexora_border)
        );

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(16), dp(18), dp(16));

        TextView tvName = createText(
                name,
                18,
                R.color.nexora_text_primary,
                true
        );

        TextView tvDetails = createText(
                "Code: " + code
                        + "\nCourse: " + course
                        + "\nSemester: " + semester,
                14,
                R.color.nexora_text_secondary,
                false
        );

        LinearLayout.LayoutParams detailsParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        detailsParams.topMargin = dp(8);
        tvDetails.setLayoutParams(detailsParams);

        MaterialButton btnDelete = new MaterialButton(this);
        btnDelete.setText("Delete Subject");
        btnDelete.setAllCaps(false);
        btnDelete.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        0xFFDC2626
                )
        );

        LinearLayout.LayoutParams deleteParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        deleteParams.topMargin = dp(12);
        btnDelete.setLayoutParams(deleteParams);

        btnDelete.setOnClickListener(v ->
                confirmDelete(document.getId(), name)
        );

        content.addView(tvName);
        content.addView(tvDetails);
        content.addView(btnDelete);

        card.addView(content);
        subjectContainer.addView(card);
    }

    private void confirmDelete(String documentId, String subjectName) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Subject")
                .setMessage(
                        "Are you sure you want to delete "
                                + subjectName + "?"
                )
                .setNegativeButton("Cancel", (dialog, which) ->
                        dialog.dismiss()
                )
                .setPositiveButton("Delete", (dialog, which) ->
                        deleteSubject(documentId)
                )
                .show();
    }

    private void deleteSubject(String documentId) {
        db.collection("subjects")
                .document(documentId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(
                            this,
                            "Subject deleted.",
                            Toast.LENGTH_SHORT
                    ).show();

                    loadSubjects();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Delete failed: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void setLoading(boolean loading) {
        findViewById(R.id.btnAddSubject).setEnabled(!loading);
        progressSubjects.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );
    }

    private String getText(TextInputEditText input) {
        return input.getText() == null
                ? ""
                : input.getText().toString().trim();
    }

    private String getStringValue(
            DocumentSnapshot document,
            String field
    ) {
        String value = document.getString(field);

        return value == null || value.trim().isEmpty()
                ? "Not provided"
                : value;
    }

    private TextView createText(
            String text,
            int size,
            int colorResource,
            boolean bold
    ) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(getColor(colorResource));

        if (bold) {
            view.setTypeface(
                    null,
                    android.graphics.Typeface.BOLD
            );
        }

        return view;
    }

    private int dp(int value) {
        return (int) (
                value * getResources()
                        .getDisplayMetrics().density
        );
    }
}
