package com.shiptrack.shiptrack_pro.service;
 
import com.shiptrack.shiptrack_pro.dto.LoginRequest;
import com.shiptrack.shiptrack_pro.dto.LoginResponse;
import com.shiptrack.shiptrack_pro.dto.RegisterRequest;
import com.shiptrack.shiptrack_pro.dto.UserResponse;
 
import java.util.List;
 
public interface UserService {
<<<<<<< HEAD
    UserResponse registerUser(RegisterRequest request);
    LoginResponse loginUser(LoginRequest request);
    List<UserResponse> getAllUsers();
    UserResponse getUserProfile(Long userId);
    UserResponse updateProfile(Long userId, String fullName, String phone);
    UserResponse updateUserRole(Long userId, String newRole);
    LoginResponse loginWithOAuth(String email, String fullName);
=======
	UserResponse registerUser(RegisterRequest request);
	LoginResponse loginUser(LoginRequest request);
	List<UserResponse> getAllUsers();
	UserResponse getUserProfile(Long userId);
	UserResponse updateProfile(Long userId, String fullName, String phone);
	UserResponse updateUserRole(Long userId, String newRole);
	UserResponse updateUserStatus(Long userId, String status);
	LoginResponse loginWithOAuth(String email, String fullName);
>>>>>>> 34cebd2b62c2f4da8d42fdd65362427708fb6412
}
