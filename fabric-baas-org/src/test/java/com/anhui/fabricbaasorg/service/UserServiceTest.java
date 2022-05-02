package com.anhui.fabricbaasorg.service;

import com.anhui.fabricbaascommon.exception.IncorrectPasswordException;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
class UserServiceTest {
    @Autowired
    private UserService userService;

    @Test
    void test() throws IncorrectPasswordException {
        String token = userService.login("admin", "12345678");
        System.out.println(token);
    }
}