package com.nowcoder.community.controller.interceptor;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.util.HostHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class MessageInterceptor implements HandlerInterceptor {

    private HostHolder hostHolder;
    private MessageService messageService;

    public MessageInterceptor(HostHolder hostHolder, MessageService messageService) {
        this.hostHolder = hostHolder;
        this.messageService = messageService;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user = hostHolder.getUser();
        if (user != null && modelAndView != null) {
            // 只要登录了，任何没有被拦截器拦截的网页中都可以直接用 $loginUser 访问当前登录的user
            int unreadMsgCount = messageService.findLetterUnreadCount(user.getId(), null) + messageService.findNoticeUnreadCount(user.getId(), null);
            modelAndView.addObject("headerUnreadMsgCount", unreadMsgCount);
        }
    }
}
