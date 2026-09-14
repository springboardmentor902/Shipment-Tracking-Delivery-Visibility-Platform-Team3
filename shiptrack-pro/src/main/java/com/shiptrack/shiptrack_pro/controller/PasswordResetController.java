package com.shiptrack.shiptrack_pro.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(
        origins = {
                "http://localhost:3000",
                "http://localhost:3001"
        },
        allowCredentials = "true"
)
public class PasswordResetController {

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @RequestBody ForgotPasswordRequest request
    ) {

        String email = request.getEmail();

        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "message",
                            "Email is required"
                    )
            );
        }

        /*
         * TODO:
         * Add your email/token generation logic here.
         *
         * For now, this confirms that the endpoint is working.
         */

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "If this email exists, a password reset token will be sent."
                )
        );
    }

    public static class ForgotPasswordRequest {

        private String email;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}