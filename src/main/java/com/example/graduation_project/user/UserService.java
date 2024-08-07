package com.example.graduation_project.user;

import com.example.graduation_project.user.model.User;
import com.example.graduation_project.util.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import static com.example.graduation_project.util.BaseResponseStatus.*;

@Slf4j
@Service
public class UserService {

    private final UserProvider userProvider;
    private final UserDao userDao;

    @Autowired
    public UserService( UserProvider userProvider, UserDao userDao) {
        this.userProvider = userProvider;
        this.userDao = userDao;
    }

    public User createUser(User user) throws BaseException {
        if (userProvider.checkEmail(user.getEmail()) == 1)
            throw new BaseException(POST_USERS_EXISTS_EMAIL);
        try {
            return this.userDao.insertUser(user);
        } catch (Exception e) {
            e.printStackTrace();
            throw new BaseException(DATABASE_ERROR);
        }

    }

}
