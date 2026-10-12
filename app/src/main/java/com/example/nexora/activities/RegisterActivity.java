package com.example.nexora.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout nameLayout, emailLayout;
    private TextInputLayout studentIdLayout, courseLayout;
    private TextInputLayout passwordLayout, confirmPasswordLayout;

    private TextInputEditText etName, etEmail;
    private TextInputEditText etStudentId, etCourse;
    private TextInputEditText etPassword, etConfirmPassword;

    private MaterialButton btnRegister;
    private ProgressBar progressRegister;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Connect XML fields
        nameLayout = findViewById(R.id.nameLayout);
        emailLayout = findViewById(R.id.registerEmailLayout);
        studentIdLayout = findViewById(R.id.studentIdLayout);
        courseLayout = findViewById(R.id.courseLayout);
        passwordLayout = findViewById(R.id.registerPasswordLayout);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etRegisterEmail);
        etStudentId = findViewById(R.id.etStudentId);
        etCourse = findViewById(R.id.etCourse);
        etPassword = findViewById(R.id.etRegisterPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        btnRegister = findViewById(R.id.btnRegister);
        progressRegister = findViewById(R.id.progressRegister);

        TextView tvBackToLogin = findViewById(R.id.tvBackToLogin);

        // Firebase instances
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvBackToLogin.setOnClickListener(v -> finish());
        btnRegister.setOnClickListener(v -> validateForm());
    }

    private void validateForm() {
        clearErrors();

        String name = getText(etName);
        String email = getText(etEmail);
        String studentId = getText(etStudentId);
        String course = getText(etCourse);
        String password = getText(etPassword);
        String confirmPassword = getText(etConfirmPassword);

        boolean valid = true;

        if (TextUtils.isEmpty(name)) {
            nameLayout.setError("Enter your full name");
            valid = false;
        }

        if (TextUtils.isEmpty(email)) {
            emailLayout.setError("Enter your email");
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Enter a valid email address");
            valid = false;
        }

        if (TextUtils.isEmpty(studentId)) {
            studentIdLayout.setError("Enter your student ID");
            valid = false;
        }

        if (TextUtils.isEmpty(course)) {
            courseLayout.setError("Enter your course");
            valid = false;
        }

        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("Enter a password");
            valid = false;
        } else if (password.length() < 6) {
            passwordLayout.setError("Use at least 6 characters");
            valid = false;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordLayout.setError("Confirm your password");
            valid = false;
        } else if (!password.equals(confirmPassword)) {
            confirmPasswordLayout.setError("Passwords do not match");
            valid = false;
        }

        if (!valid) {
            Toast.makeText(this,
                    "Please correct the highlighted fields",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        createFirebaseAccount(name, email, studentId, course, password);
    }

    private void createFirebaseAccount(
            String name,
            String email,
            String studentId,
            String course,
            String password) {

        setLoading(true);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (!task.isSuccessful()) {
                        setLoading(false);

                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : "Registration failed. Please try again.";

                        Toast.makeText(this, message,
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    FirebaseUser firebaseUser = auth.getCurrentUser();

                    if (firebaseUser == null) {
                        setLoading(false);
                        Toast.makeText(this,
                                "Account created, but user details could not be loaded.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    String userId = firebaseUser.getUid();

                    Map<String, Object> user = new HashMap<>();
                    user.put("userId", userId);
                    user.put("name", name);
                    user.put("email", email);
                    user.put("studentId", studentId);
                    user.put("course", course);
                    user.put("role", "student");
                    user.put("approvalStatus", "pending");
                    user.put("createdAt", FieldValue.serverTimestamp());

                    db.collection("users")
                            .document(userId)
                            .set(user)
                            .addOnSuccessListener(unused -> {
                                setLoading(false);

                                // Do not leave the newly registered
                                // student signed in automatically.
                                auth.signOut();

                                Toast.makeText(this,
                                        "Registration submitted! Await admin approval.",
                                        Toast.LENGTH_LONG).show();

                                Intent intent = new Intent(
                                        RegisterActivity.this,
                                        LoginActivity.class);
                                intent.addFlags(
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                                                | Intent.FLAG_ACTIVITY_NEW_TASK
                                                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                // The Auth account exists, but its profile
                                // could not be saved. Remove the new account
                                // where possible to avoid a partial signup.
                                FirebaseUser newUser = auth.getCurrentUser();

                                if (newUser != null) {
                                    newUser.delete().addOnCompleteListener(
                                            deleteTask -> {
                                                auth.signOut();
                                                setLoading(false);

                                                String message =
                                                        "Could not save your profile. "
                                                                + "Please try again. "
                                                                + "If the issue continues, contact support.";

                                                Toast.makeText(
                                                        RegisterActivity.this,
                                                        message,
                                                        Toast.LENGTH_LONG).show();
                                            });
                                } else {
                                    auth.signOut();
                                    setLoading(false);
                                    Toast.makeText(this,
                                            "Could not save your profile. Please try again.",
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                });
    }

    private void clearErrors() {
        nameLayout.setError(null);
        emailLayout.setError(null);
        studentIdLayout.setError(null);
        courseLayout.setError(null);
        passwordLayout.setError(null);
        confirmPasswordLayout.setError(null);
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() == null
                ? ""
                : editText.getText().toString().trim();
    }

    private void setLoading(boolean loading) {
        progressRegister.setVisibility(
                loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
        btnRegister.setText(
                loading ? "Submitting..." : "Request Registration");
    }
}

