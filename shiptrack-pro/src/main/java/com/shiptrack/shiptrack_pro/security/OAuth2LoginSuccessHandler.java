package com.shiptrack.shiptrack_pro.security;

import java.io.IOException;

import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import tools.jackson.databind.json.JsonMapper;

import com.shiptrack.shiptrack_pro.dto.LoginResponse;
import com.shiptrack.shiptrack_pro.service.UserService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@Component
public class OAuth2LoginSuccessHandler
        implements AuthenticationSuccessHandler {

    private final UserService userService;
    private final JsonMapper jsonMapper;


    public OAuth2LoginSuccessHandler(
            @Lazy UserService userService,
            JsonMapper jsonMapper) {

        this.userService = userService;
        this.jsonMapper = jsonMapper;
    }


    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        OAuth2User oauthUser =
                (OAuth2User) authentication.getPrincipal();


        String login =
                oauthUser.getAttribute("login");

        String name =
                oauthUser.getAttribute("name");

        String email =
                oauthUser.getAttribute("email");


        System.out.println(
                "===== GitHub OAuth Login Successful =====");

        System.out.println(
                "GitHub Login: " + login);

        System.out.println(
                "Name: " + name);

        System.out.println(
                "Email: " + email);


        if (email == null || email.isBlank()) {

            response.setStatus(
                    HttpServletResponse.SC_BAD_REQUEST);

            response.setContentType(
                    "application/json");

            response.getWriter().write(
                    "{\"message\":\"GitHub account email is not available.\"}"
            );

            return;
        }


        LoginResponse loginResponse =
                userService.loginWithOAuth(
                        email,
                        name);


        response.setStatus(
                HttpServletResponse.SC_OK);

        response.setContentType(
                "application/json");

        response.setCharacterEncoding(
                "UTF-8");


        jsonMapper.writeValue(
                response.getWriter(),
                loginResponse);
    }
}