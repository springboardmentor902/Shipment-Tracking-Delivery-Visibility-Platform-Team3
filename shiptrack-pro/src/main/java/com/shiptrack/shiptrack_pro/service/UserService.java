package com.shiptrack.shiptrack_pro.service;
 
import com.shiptrack.shiptrack_pro.dto.LoginRequest;
import com.shiptrack.shiptrack_pro.dto.LoginResponse;
import com.shiptrack.shiptrack_pro.dto.RegisterRequest;
import com.shiptrack.shiptrack_pro.dto.UserResponse;
import com.shiptrack.shiptrack_pro.dto.ChangePasswordRequest;
import com.shiptrack.shiptrack_pro.dto.ForgotPasswordRequest;
import com.shiptrack.shiptrack_pro.dto.ForgotPasswordResponse;
import com.shiptrack.shiptrack_pro.dto.ProfileUpdateRequest;
import com.shiptrack.shiptrack_pro.dto.ResetPasswordRequest;
 
import java.util.List;
 
public interface UserService {
    UserResponse registerUser(RegisterRequest request);
    LoginResponse loginUser(LoginRequest request);
    List<UserResponse> getAllUsers();
    UserResponse updateUserRole(Long userId, String newRole);
    UserResponse getCurrentUserProfile();
    UserResponse updateCurrentUserProfile(ProfileUpdateRequest request);
    void changeCurrentUserPassword(ChangePasswordRequest request);
    ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
}
