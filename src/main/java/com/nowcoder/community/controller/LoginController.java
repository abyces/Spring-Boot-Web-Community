package com.nowcoder.community.controller;

import com.google.code.kaptcha.Producer;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.AuthCodeHolder;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.RedisKeyUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class LoginController implements CommunityConstant {

    @Value("${server.servlet.context-path}")
    private String contextPath;
    private UserService userService;
    private Producer kaptchaProducer;
    private AuthCodeHolder authCodeHolder;
    private RedisTemplate redisTemplate;
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    public LoginController(UserService userService, Producer kaptchaProducer, AuthCodeHolder authCodeHolder, RedisTemplate redisTemplate) {
        this.userService = userService;
        this.kaptchaProducer = kaptchaProducer;
        this.authCodeHolder = authCodeHolder;
        this.redisTemplate = redisTemplate;
    }

    @RequestMapping(path = "/register", method = RequestMethod.GET)
    public String getRegisterPage() {
        return "/site/register";
    }

    @RequestMapping(path = "/login", method = RequestMethod.GET)
    public String getLoginPage() {
        return "/site/login";
    }

    @RequestMapping(path = "/forget", method = RequestMethod.GET)
    public String getForgetPage() {
        return "/site/forget";
    }

    @RequestMapping(path = "/register", method = RequestMethod.POST)
    public String register(Model model, User user) throws IllegalAccessException {
        Map<String, Object> map = userService.register(user);
        if (map == null || map.isEmpty()) {
            model.addAttribute("msg", "Registration Succeeded.");
            model.addAttribute("target", "/index");
            return "/site/operate-result";
        } else {
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            model.addAttribute("emailMsg", map.get("emailMsg"));
            return "/site/register";
        }
    }

    // http://localhost:8080/community/activation/{id}/{code}
    @RequestMapping(path = "/activation/{userId}/{activationCode}", method = RequestMethod.GET)
    public String activation(Model model,
                             @PathVariable("userId") int userId,
                             @PathVariable("activationCode") String activationCode) {
        int result = userService.activation(userId, activationCode);
        if (result == ACTIVATION_SUCCESS) {
            model.addAttribute("msg", "Activation Succeed.");
            model.addAttribute("target", "/login");
        } else if (result == ACTIVATION_REPEAT) {
            model.addAttribute("msg", "Activation Repeated.");
            model.addAttribute("target", "/index");
        } else {
            model.addAttribute("msg", "Activation Failed.");
            model.addAttribute("target", "/index");
        }

        return "/site/operate-result";
    }

    @RequestMapping(path = "/kaptcha", method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response/*, HttpSession session */) {
        // generate verification code
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);

        // store in session
//        session.setAttribute("kaptcha", text);

        // 生成随机字符串标识用户
        String kaptchaOwner = CommunityUtil.generateUUID();
        Cookie cookie = new Cookie("kaptchaOwner", kaptchaOwner);
        cookie.setMaxAge(60);
        cookie.setPath(contextPath);
        response.addCookie(cookie);
        // 存入redis
        String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        redisTemplate.opsForValue().set(redisKey, text, 60, TimeUnit.SECONDS);

        // send image to browser
        response.setContentType("image/png");
        try {
            OutputStream os = response.getOutputStream();
            ImageIO.write(image, "png", os);
        } catch (IOException e) {
            logger.error("Kaptcha error!");
        }
    }

    @RequestMapping(path = "/login", method = RequestMethod.POST)
    public String login(String username, String password, String verifyCode, boolean rememberMe,
                        Model model, /* HttpSession session, */ HttpServletResponse response,
                        @CookieValue("kaptchaOwner") String kaptchaOwner) {
//        String kaptcha = (String) session.getAttribute("kaptcha");

        // 从Redis中取验证码
        String kaptcha = null;
        // 先判断cookie是否失效
        if (StringUtils.isNotBlank(kaptchaOwner)) {
            String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
            kaptcha = (String) redisTemplate.opsForValue().get(redisKey);
        }


        if (StringUtils.isBlank(kaptcha) || StringUtils.isBlank(verifyCode) || !kaptcha.equalsIgnoreCase(verifyCode)) {
            logger.debug("login: Wrong verifyCode");
            model.addAttribute("codeMsg", "Verification Code wrong!");
            return "/site/login";
        }

        int expiredSeconds = rememberMe ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
        Map<String, Object> map = userService.login(username, password, expiredSeconds);
        if (map.containsKey("ticket")) {
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
            cookie.setPath(contextPath);
            cookie.setMaxAge(expiredSeconds);
            response.addCookie(cookie);
            return "redirect:/index";
        } else {
            logger.debug("login: Error. usernameMsg: " + map.get("usernameMsg") + ", passwordMsg: " + map.get("passwordMsg"));
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/login";
        }
    }

    @RequestMapping(path = "/logout", method = RequestMethod.GET)
    public String logout(@CookieValue("ticket") String ticket, Model model) {
        userService.logout(ticket);
        model.addAttribute("msg", "Success!");
        model.addAttribute("target", "/login");
        return "/site/operate-result";
    }

    @RequestMapping(path = "/getAuthCode", method = RequestMethod.POST)
    @ResponseBody
    public String getAuthCode(String email) {
        if (email == null || StringUtils.isBlank(email)) {
            return CommunityUtil.getJSONString(1, "Empty email!");
        }

        String authCode = CommunityUtil.generateUUID().substring(0, 5);
        userService.sendAuthorizeEmail(email, authCode);
        authCodeHolder.setCode(email, authCode);

        return CommunityUtil.getJSONString(0, "Success.");
    }

    @RequestMapping(path = "/reset-password", method = RequestMethod.POST)
    public String resetPassword(String email, String verifycode, String password, Model model) {
        String authCode = authCodeHolder.getAuthCode(email);
        if (verifycode == null || StringUtils.isBlank(verifycode) || authCode == null || !authCode.equals(verifycode)) {
            logger.debug("Forget Password: Wrong verifyCode");
            model.addAttribute("codeMsg", "Verification Code wrong!");
            return "/site/forget";
        }

        authCodeHolder.clear(email);
        Map<String, Object> map = userService.setForgottenPassword(email, password);
        if (map.isEmpty()) {
            model.addAttribute("msg", "Success!");
            model.addAttribute("target", "/login");
            return "/site/operate-result";
        } else {
            model.addAttribute("emailMsg", map.get("emailMsg"));
            return "/site/forget";
        }
    }

}
