package com.example.nexora.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nexora.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();

        // Open Student Approvals
        MaterialButton btnStudentApprovals =
                findViewById(R.id.btnStudentApprovals);

        btnStudentApprovals.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivity.this,
                    StudentApprovalActivity.class
            );
            startActivity(intent);
        });

        // Open Subject Management
        MaterialButton btnManageSubjects =
                findViewById(R.id.btnManageSubjects);

        btnManageSubjects.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivity.this,
                    SubjectManagementActivity.class
            );
            startActivity(intent);
        });

        // Open Attendance Management
        MaterialButton btnManageAttendance = findViewById(R.id.btnManageAttendance);

        btnManageAttendance.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivity.this,
                    AttendanceManagementActivity.class
            );
            startActivity(intent);
        });

        //mark management
        MaterialButton btnManageMarks = findViewById(R.id.btnManageMarks);

        btnManageMarks.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivity.this,
                    MarksManagementActivity.class
            );
            startActivity(intent);
        });

        //Announcement

        MaterialButton btnManageAnnouncements =
                findViewById(R.id.btnManageAnnouncements);

        btnManageAnnouncements.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivity.this,
                    ManageAnnouncementsActivity.class
            );
            startActivity(intent);
        });



        // Admin logout
        MaterialButton tvLogout =
                findViewById(R.id.tvAdminLogout);

        tvLogout.setOnClickListener(v -> {
            auth.signOut();

            Toast.makeText(
                    MainActivity.this,
                    "Logged out successfully",
                    Toast.LENGTH_SHORT
            ).show();

            Intent intent = new Intent(
                    MainActivity.this,
                    LoginActivity.class
            );

            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);
            finish();
        });
    }
}
