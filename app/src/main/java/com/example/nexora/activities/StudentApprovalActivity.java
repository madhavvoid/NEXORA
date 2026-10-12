package com.example.nexora.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nexora.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class StudentApprovalActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private LinearLayout approvalContainer;
    private ProgressBar progressApprovals;
    private TextView tvApprovalStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_approval);

        approvalContainer = findViewById(R.id.approvalContainer);
        progressApprovals = findViewById(R.id.progressApprovals);
        tvApprovalStatus = findViewById(R.id.tvApprovalStatus);

        db = FirebaseFirestore.getInstance();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadPendingStudents();
    }

    private void loadPendingStudents() {
        progressApprovals.setVisibility(View.VISIBLE);
        tvApprovalStatus.setVisibility(View.GONE);
        approvalContainer.removeAllViews();

        db.collection("users")
                .whereEqualTo("role", "student")
                .whereEqualTo("approvalStatus", "pending")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    progressApprovals.setVisibility(View.GONE);
                    approvalContainer.removeAllViews();

                    if (querySnapshot.isEmpty()) {
                        tvApprovalStatus.setText(
                                "No pending registrations found."
                        );
                        tvApprovalStatus.setVisibility(View.VISIBLE);
                        return;
                    }

                    tvApprovalStatus.setVisibility(View.GONE);

                    for (DocumentSnapshot document : querySnapshot) {
                        addStudentCard(document);
                    }
                })
                .addOnFailureListener(e -> {
                    progressApprovals.setVisibility(View.GONE);
                    tvApprovalStatus.setText(
                            "Could not load students. Check your connection and try again."
                    );
                    tvApprovalStatus.setVisibility(View.VISIBLE);

                    Toast.makeText(
                            this,
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void addStudentCard(DocumentSnapshot document) {
        String name = getValue(document, "name");
        String email = getValue(document, "email");
        String studentId = getValue(document, "studentId");
        String course = getValue(document, "course");

        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.bottomMargin = dp(16);
        card.setLayoutParams(cardParams);
        card.setRadius(dp(16));
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
        content.setPadding(dp(18), dp(18), dp(18), dp(18));

        TextView tvName = makeText(
                name,
                19,
                R.color.nexora_text_primary,
                true
        );

        TextView tvDetails = makeText(
                "Email: " + email
                        + "\nStudent ID: " + studentId
                        + "\nCourse: " + course,
                14,
                R.color.nexora_text_secondary,
                false
        );

        LinearLayout.LayoutParams detailsParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        detailsParams.topMargin = dp(10);
        tvDetails.setLayoutParams(detailsParams);

        content.addView(tvName);
        content.addView(tvDetails);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams buttonsParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        buttonsParams.topMargin = dp(18);
        buttons.setLayoutParams(buttonsParams);

        MaterialButton btnApprove = new MaterialButton(this);
        btnApprove.setText("Approve");
        btnApprove.setAllCaps(false);
        btnApprove.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        getColor(R.color.nexora_blue)
                )
        );

        MaterialButton btnReject = new MaterialButton(this);
        btnReject.setText("Reject");
        btnReject.setAllCaps(false);
        btnReject.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        0xFFDC2626
                )
        );

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );
        buttonParams.setMarginEnd(dp(8));
        btnApprove.setLayoutParams(buttonParams);

        LinearLayout.LayoutParams rejectParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );
        btnReject.setLayoutParams(rejectParams);

        btnApprove.setOnClickListener(v ->
                updateApproval(
                        document.getId(),
                        "approved",
                        btnApprove,
                        btnReject
                )
        );

        btnReject.setOnClickListener(v ->
                updateApproval(
                        document.getId(),
                        "rejected",
                        btnApprove,
                        btnReject
                )
        );

        buttons.addView(btnApprove);
        buttons.addView(btnReject);

        content.addView(buttons);
        card.addView(content);
        approvalContainer.addView(card);
    }

    private void updateApproval(
            String userId,
            String newStatus,
            MaterialButton btnApprove,
            MaterialButton btnReject
    ) {
        btnApprove.setEnabled(false);
        btnReject.setEnabled(false);

        db.collection("users")
                .document(userId)
                .update("approvalStatus", newStatus)
                .addOnSuccessListener(unused -> {
                    String message = newStatus.equals("approved")
                            ? "Student approved successfully."
                            : "Student registration rejected.";

                    Toast.makeText(
                            this,
                            message,
                            Toast.LENGTH_SHORT
                    ).show();

                    loadPendingStudents();
                })
                .addOnFailureListener(e -> {
                    btnApprove.setEnabled(true);
                    btnReject.setEnabled(true);

                    Toast.makeText(
                            this,
                            "Could not update student: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private TextView makeText(
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
            view.setTypeface(null, android.graphics.Typeface.BOLD);
        }

        return view;
    }

    private String getValue(
            DocumentSnapshot document,
            String field
    ) {
        String value = document.getString(field);
        return value == null || value.trim().isEmpty()
                ? "Not provided"
                : value;
    }

    private int dp(int value) {
        return (int) (
                value * getResources()
                        .getDisplayMetrics().density
        );
    }
}
