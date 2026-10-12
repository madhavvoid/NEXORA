package com.example.nexora.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nexora.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;

    private TextInputEditText etEmail;
    private TextInputEditText etPassword;

    private MaterialButton btnLogin;
    private ProgressBar progressLogin;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        btnLogin = findViewById(R.id.btnLogin);
        progressLogin = findViewById(R.id.progressLogin);

        TextView tvCreateAccount =
                findViewById(R.id.tvCreateAccount);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvCreateAccount.setOnClickListener(v -> {
            Intent intent = new Intent(
                    LoginActivity.this,
                    RegisterActivity.class
            );
            startActivity(intent);
        });

        btnLogin.setOnClickListener(v -> loginUser());
    }

    private void loginUser() {
        emailLayout.setError(null);
        passwordLayout.setError(null);

        String email = getText(etEmail);
        String password = getText(etPassword);

        boolean valid = true;

        if (email.isEmpty()) {
            emailLayout.setError("Enter your email");
            valid = false;
        }

        if (password.isEmpty()) {
            passwordLayout.setError("Enter your password");
            valid = false;
        }

        if (!valid) {
            return;
        }

        setLoading(true);

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        setLoading(false);

                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : "Login failed. Please try again.";

                        Toast.makeText(
                                this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    if (auth.getCurrentUser() == null) {
                        setLoading(false);
                        auth.signOut();
                        Toast.makeText(
                                this,
                                "Could not verify your account.",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    String uid = auth.getCurrentUser().getUid();

                    db.collection("users")
                            .document(uid)
                            .get()
                            .addOnSuccessListener(document -> {
                                if (!document.exists()) {
                                    rejectLogin(
                                            "Your user profile was not found."
                                    );
                                    return;
                                }

                                String role =
                                        document.getString("role");

                                String status =
                                        document.getString(
                                                "approvalStatus"
                                        );

                                if ("admin".equals(role)) {
                                    openDashboard(true);
                                } else if ("student".equals(role)
                                        && "approved".equals(status)) {
                                    openDashboard(false);
                                } else if ("student".equals(role)
                                        && "pending".equals(status)) {
                                    rejectLogin(
                                            "Your registration is awaiting admin approval."
                                    );
                                } else if ("student".equals(role)
                                        && "rejected".equals(status)) {
                                    rejectLogin(
                                            "Your registration was not approved. Contact the administrator."
                                    );
                                } else {
                                    rejectLogin(
                                            "Your account role or approval status is invalid."
                                    );
                                }
                            })
                            .addOnFailureListener(e ->
                                    rejectLogin(
                                            "Could not verify your profile. Check your connection and try again."
                                    )
                            );
                });
    }

    private void openDashboard(boolean isAdmin) {
        setLoading(false);

        Intent intent;

        if (isAdmin) {
            intent = new Intent(
                    LoginActivity.this,
                    MainActivity.class
            );
        } else {
            intent = new Intent(
                    LoginActivity.this,
                    StudentDashboardActivity.class
            );
        }

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private void rejectLogin(String message) {
        auth.signOut();
        setLoading(false);

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() == null
                ? ""
                : editText.getText().toString().trim();
    }

    private void setLoading(boolean loading) {
        progressLogin.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "Signing in..." : "Sign In");
    }
}
