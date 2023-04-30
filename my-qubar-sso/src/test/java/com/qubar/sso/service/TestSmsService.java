package com.qubar.sso.service;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class  TestSmsService {

    @Autowired
    private SmsService smsService;

    @Test
    public void testSendSms() throws Exception {
        String captcha = this.smsService.sendSms("15882320653");
        System.out.println(captcha);
    }

}
