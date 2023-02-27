package com.nowcoder.community.service;

import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.MailClient;
import com.nowcoder.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements CommunityConstant {
    private UserMapper userMapper;
    private LoginTicketMapper loginTicketMapper;
    private MailClient mailClient;
    private TemplateEngine templateEngine;
    private RedisTemplate redisTemplate;

    @Value("${community.path.domain}")
    private String domain;
    @Value("${server.servlet.context-path}")
    private String contextPath;

    public UserService(UserMapper userMapper, LoginTicketMapper loginTicketMapper, MailClient mailClient, TemplateEngine templateEngine, RedisTemplate redisTemplate) {
        this.userMapper = userMapper;
        this.loginTicketMapper = loginTicketMapper;
        this.mailClient = mailClient;
        this.templateEngine = templateEngine;
        this.redisTemplate = redisTemplate;
    }

    public User findUserById(int userId) {
        User user = getCache(userId);
        if (user == null) {
            user = initCache(userId);
        }

        return user;
    }

    public User findUserByName(String username) {
        return userMapper.selectByName(username);
    }

    public User findUserByEmail(String email) {
        return userMapper.selectByEmail(email);
    }

    public Map<String, Object> register(User user) throws IllegalAccessException {
        Map<String, Object> map = new HashMap<>();

        // validation
        if (user == null) {
            throw new IllegalAccessException("Empty user");
        }
        if (StringUtils.isBlank(user.getUsername())) {
            map.put("usernameMsg", "Empty username!");
            return map;
        }
        if (StringUtils.isBlank(user.getPassword())) {
            map.put("passwordMsg", "Empty password!");
            return map;
        }
        if (StringUtils.isBlank(user.getEmail())) {
            map.put("emailMsg", "Empty e-mail!");
            return map;
        }

        if (findUserByName(user.getUsername()) != null) {
            map.put("usernameMsg", "Existed username!");
            return map;
        }

        if (findUserByEmail(user.getEmail()) != null) {
            map.put("emailMsg", "Existed email!");
            return map;
        }

        // register
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
        user.setPassword(CommunityUtil.md5(user.getPassword() + user.getSalt()));
        user.setType(0);
        user.setStatus(0);
        user.setActivationCode(CommunityUtil.generateUUID());
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        // activation email
        Context context = new Context();
        context.setVariable("email", user.getEmail());
        // http://localhost:8080/community/activation/{id}/{code}
        context.setVariable("url", domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode());
        String content = templateEngine.process("/mail/activation", context);
        mailClient.sendMail(user.getEmail(), "Activate Your Account", content);

        return map;
    }

    public int activation(int userId, String activationCode) {
        User user = userMapper.selectById(userId);
        if (user.getStatus() == 1) {
            return ACTIVATION_REPEAT;
        } else if (user.getActivationCode().equals(activationCode)) {
            userMapper.updateStatus(userId, 1);
            clearCache(userId);
            return ACTIVATION_SUCCESS;
        } else {
            return ACTIVATION_FAILURE;
        }
    }

    public Map<String, Object> login(String username, String password, long expiredSeconds) {
        Map<String, Object> map = new HashMap<>();

        // test null
        if (StringUtils.isBlank(username)) {
            map.put("usernameMsg", "Empty username!");
            return map;
        }
        if (StringUtils.isBlank(password)) {
            map.put("passwordMsg", "Empty password!");
            return map;
        }

        // test validation
        User user = findUserByName(username);
        if (user == null) {
            map.put("usernameMsg", "Not existed account!");
            return map;
        }
        if (user.getStatus() == 0) {
            map.put("usernameMsg", "Not activated account!");
            return map;
        }

        password = CommunityUtil.md5(password + user.getSalt());
        if (!user.getPassword().equals(password)) {
            map.put("passwordMsg", "Wrong password!");
            return map;
        }

        // generate login ticket
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000));
//        loginTicketMapper.insertLoginTicket(loginTicket);

        String redisKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(redisKey, loginTicket); // redis自动将对象序列化为Json

        map.put("ticket", loginTicket.getTicket());

        return map;
    }

    public void logout(String ticket) {
//        loginTicketMapper.updateStatus(ticket, 1);

        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(redisKey);
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(redisKey, loginTicket);

    }

    public LoginTicket findLoginTicket(String ticket) {
//        return loginTicketMapper.selectByTicket(ticket);

        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket) redisTemplate.opsForValue().get(redisKey);

    }

    public int updateHeader(int userId, String headerUrl) {
        int rows = userMapper.updateHeader(userId, headerUrl);
        clearCache(userId);
        return rows;
    }

    public Map<String, Object> updatePassword(int userId, String oldPassword, String newPassword, String confirmPassword) {
        Map<String, Object> map = new HashMap<>();

        if (oldPassword == null || StringUtils.isBlank(oldPassword)) {
            map.put("oldPasswordMsg", "Empty password!");
            return map;
        }
        if (newPassword == null || StringUtils.isBlank(newPassword)) {
            map.put("newPasswordMsg", "Empty password!");
            return map;
        }

        if (confirmPassword == null || StringUtils.isBlank(confirmPassword) || !newPassword.equals(confirmPassword)) {
            map.put("confirmPasswordMsg", "Re-entered password not match!");
            return map;
        }

        User user = findUserById(userId);

        // check old password == password in database
        if (!CommunityUtil.md5(oldPassword + user.getSalt()).equals(user.getPassword())) {
            map.put("oldPasswordMsg", "Password not match!");
            return map;
        }

        // if update fails, add error msg
        if (userMapper.updatePassword(userId, CommunityUtil.md5(newPassword + user.getSalt())) == 0) {
            map.put("newPasswordMsg", "Update Failure!");
        }

        if (map.isEmpty()) clearCache(userId);

        return map;
    }

    public void sendAuthorizeEmail(String email, String authCode) {
        // send reset link to email
        Context context = new Context();
        context.setVariable("email", email);
        context.setVariable("code", authCode);
        String content = templateEngine.process("/mail/forget", context);
        mailClient.sendMail(email, "Reset your password", content);
    }

    public Map<String, Object> setForgottenPassword(String email, String password) {
        Map<String, Object> map = new HashMap<>();

        User user = userMapper.selectByEmail(email);
        if (user == null) {
            map.put("emailMsg", "This email not existed before!");
            return map;
        }

        userMapper.updatePassword(user.getId(), CommunityUtil.md5(password + user.getSalt()));
        return map;
    }

    // 尝试从redis中取，否则再从数据库载入redis；修改时直接删除redis中的缓存
    private User getCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(redisKey);
    }

    private User initCache(int userId) {
        User user = userMapper.selectById(userId);
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(redisKey, user, 3600, TimeUnit.SECONDS);
        return user;
    }

    private void clearCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(redisKey);
    }

}
