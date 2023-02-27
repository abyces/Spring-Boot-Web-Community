package com.nowcoder.community;

import com.nowcoder.community.util.MailClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class MailTests {

    private MailClient mailClient;

    private TemplateEngine templateEngine;

    @Autowired
    public void setTemplateEngine(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Autowired
    public void setMailClient(MailClient mailClient) {
        this.mailClient = mailClient;
    }

    @Test
    public void testTextMail() {
        mailClient.sendMail("624316986@qq.com", "Happy New Year!", ":)");
    }

    @Test
    public void testHtmlMail() {
        Context context = new Context();
        context.setVariable("username", "OoO");
        String content = templateEngine.process("/mail/demo", context);
        mailClient.sendMail("624316986@qq.com", "Testing HTML Content", content);
    }

    @Test
    public void testActivation() {
        Context context = new Context();
        context.setVariable("email", "624316986@qq.com");
        context.setVariable("url", "/152/7f4a78e4802b45adad6dd1e089c4f728");
//        context.setVariable("url", "null");
        String content = templateEngine.process("/mail/demo2", context);
        mailClient.sendMail("624316986@qq.com", "Testing Email Service", content);
    }
}
