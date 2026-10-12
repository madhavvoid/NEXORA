
package com.example.nexora.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nexora.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ManageAnnouncementsActivity extends AppCompatActivity {

    private TextInputEditText etTitle;
    private TextInputEditText etMessage;
    private MaterialButton btnPublish;
    private ProgressBar progressBar;
    private TextView tvStatus;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_announcements);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etTitle = findViewById(R.id.etAnnouncementTitle);
        etMessage = findViewById(R.id.etAnnouncementMessage);
        btnPublish = findViewById(R.id.btnPublishAnnouncement);
        progressBar = findViewById(R.id.progressAnnouncement);
        tvStatus = findViewById(R.id.tvAnnouncementStatus);

        findViewById(R.id.btnAnnouncementsBack).setOnClickListener(v ->
                finish()
        );

        btnPublish.setOnClickListener(v -> publishAnnouncement());
    }

    private void publishAnnouncement() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(
                    this,
                    "Please sign in again.",
                    Toast.LENGTH_SHORT
            ).show();
            finish();
            return;
        }

        String title = etTitle.getText() == null
                ? "" : etTitle.getText().toString().trim();

        String message = etMessage.getText() == null
                ? "" : etMessage.getText().toString().trim();

        if (title.isEmpty()) {
            etTitle.setError("Enter an announcement title");
            etTitle.requestFocus();
            return;
        }

        if (message.isEmpty()) {
            etMessage.setError("Enter the announcement message");
            etMessage.requestFocus();
            return;
        }

        if (title.length() > 100 || message.length() > 2000) {
            tvStatus.setText("The title or message is too long.");
            return;
        }

        setLoading(true);
        tvStatus.setText("Publishing...");

        Map<String, Object> announcement = new HashMap<>();
        announcement.put("title", title);
        announcement.put("message", message);
        announcement.put("createdAt", FieldValue.serverTimestamp());
        announcement.put("createdBy", auth.getCurrentUser().getUid());

        db.collection("announcements")
                .add(announcement)
                .addOnSuccessListener(documentReference -> {
                    setLoading(false);
                    tvStatus.setText("Announcement published successfully.");

                    etTitle.setText("");
                    etMessage.setText("");

                    Toast.makeText(
                            this,
                            "Announcement published.",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    tvStatus.setText(
                            "Could not publish. Check your connection and access rules."
                    );
                    Toast.makeText(
                            this,
                            "Publish failed: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnPublish.setEnabled(!loading);
        etTitle.setEnabled(!loading);
        etMessage.setEnabled(!loading);
    }
}
