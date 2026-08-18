package com.shiptrack.shiptrack_pro.security;

import com.shiptrack.shiptrack_pro.entity.User;
import com.shiptrack.shiptrack_pro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Resolves the caller behind the current request into a real User row.
 *
 * This is what makes "every action is tied to a user" true in practice:
 * ownership fields are always taken from the JWT-backed security context,
 * never from the request body.
 */
@Component
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    /** The authenticated user, or 401 if the request is anonymous. */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user no longer exists: " + email));
    }

    // true when a real logged in user is behind the request
    public boolean isLoggedIn() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
    }

    /** The authenticated user's role as an enum. */
    public Role getCurrentRole() {
        return Role.valueOf(getCurrentUser().getRole());
    }

    public boolean hasRole(Role... roles) {
        Role current = getCurrentRole();
        for (Role role : roles) {
            if (current == role) {
                return true;
            }
        }
        return false;
    }
}