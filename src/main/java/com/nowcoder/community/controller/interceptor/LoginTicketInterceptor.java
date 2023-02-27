package com.nowcoder.community.controller.interceptor;

import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CookieUtil;
import com.nowcoder.community.util.HostHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.Date;

@Component
public class LoginTicketInterceptor implements HandlerInterceptor {

    private UserService userService;
    private MessageService messageService;
    private HostHolder hostHolder;

    public LoginTicketInterceptor(UserService userService, MessageService messageService, HostHolder hostHolder) {
        this.userService = userService;
        this.messageService = messageService;
        this.hostHolder = hostHolder;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // get ticket from cookie
        String ticket = CookieUtil.getValue(request, "ticket");

        if (ticket != null) {
            // find user when ticket is still valid
            LoginTicket loginTicket = userService.findLoginTicket(ticket);
            if (loginTicket != null && loginTicket.getStatus() == 0 && loginTicket.getExpired().after(new Date())) {
                User user = userService.findUserById(loginTicket.getUserId());

                // keep user in this request
                // *** 因为环境是并发的，一个服务器可能同时处理多个用户请求，因此必须要做线程隔离
                // *** 使用ThreadLocal (in Util.HostHolder) 完成线程隔离
                // *** 与此同时每一次请求都是一个线程，请求完成后，对应的线程被销毁
                hostHolder.setUser(user);
            }
        }

        // return false的话后面的流程不执行
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user = hostHolder.getUser();
        if (user != null && modelAndView != null) {
            // 只要登录了，任何没有被拦截器拦截的网页中都可以直接用 $loginUser 访问当前登录的user
            modelAndView.addObject("loginUser", user);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        hostHolder.clear();
    }
}
