package com.example.Graduation_project.service;

import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = new DefaultOAuth2UserService().loadUser(userRequest);

        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");

        System.out.println("Google 사용자 정보:");
        System.out.println("이메일: " + email);
        System.out.println("이름: " + name);

        // 사용자 확인 또는 DB에 저장 로직 추가 가능
        // userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("사용자 없음"));

        return oAuth2User;
    }
}
