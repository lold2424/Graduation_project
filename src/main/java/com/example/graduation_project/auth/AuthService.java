package com.example.graduation_project.auth;

import com.example.graduation_project.auth.jwt.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthDao authDao;
    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public AuthService(AuthDao authDao, JwtTokenProvider jwtTokenProvider) {
        this.authDao = authDao;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public Long registerRefreshToken(Long userid, String refreshToken) {
        if(authDao.checkUser(userid))
            authDao.updateRefreshToken(userid, refreshToken);
        else
            authDao.insertRefreshToken(userid,refreshToken);

        return userid;
    }


}
