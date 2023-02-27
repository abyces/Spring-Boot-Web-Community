package com.nowcoder.community;

import com.nowcoder.community.service.AlphaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class TransactionTests {

    private AlphaService alphaService;

    @Autowired
    public void setAlphaService(AlphaService alphaService) {
        this.alphaService = alphaService;
    }

    @Test
    public void testSave1() {
        Object obj = alphaService.save1();
        System.out.println(obj.toString());
    }

    @Test
    public void testSave2() {
        Object obj = alphaService.save2();
        System.out.println(obj.toString());
    }

}
