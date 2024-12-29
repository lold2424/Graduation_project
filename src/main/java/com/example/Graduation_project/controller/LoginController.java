package com.example.Graduation_project.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/login")
public class LoginController {

    @GetMapping("/success")
    public String loginSuccess(@AuthenticationPrincipal OAuth2User oAuth2User) {
        return "로그인 성공! 환영합니다, " + oAuth2User.getAttribute("name") + "님!";
    }

    @GetMapping("/failure")
    public String loginFailure() {
        return "로그인 실패! 다시 시도해주세요.";
    }

    @GetMapping("/userinfo")
    public Map<String, Object> getUserInfo(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User)) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }

        OAuth2User user = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("name", user.getAttribute("name"));
        userInfo.put("email", user.getAttribute("email"));
        userInfo.put("picture", user.getAttribute("picture"));

        return userInfo;
    }
}
