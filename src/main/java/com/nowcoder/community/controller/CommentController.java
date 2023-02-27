package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.entity.*;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.HostHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.*;

@Controller
@RequestMapping("/comment")
public class CommentController implements CommunityConstant{

    private CommentService commentService;
    private HostHolder hostHolder;
    private DiscussPostService discussPostService;
    private EventProducer eventProducer;

    public CommentController(CommentService commentService, HostHolder hostHolder, DiscussPostService discussPostService, EventProducer eventProducer) {
        this.commentService = commentService;
        this.hostHolder = hostHolder;
        this.discussPostService = discussPostService;
        this.eventProducer = eventProducer;
    }

    @RequestMapping(path = "/add/{discussPostId}", method = RequestMethod.POST)
    public String addComment(@PathVariable("discussPostId") int discussPostId, Comment comment) {
        User user = hostHolder.getUser();

        comment.setUserId(user.getId());
        comment.setStatus(0);
        comment.setCreateTime(new Date());
        commentService.addComment(comment);

        // 触发comment event
        Event event = new Event()
                .setTopic(TOPIC_COMMENT)
                .setUserId(user.getId())
                .setEntityType(comment.getEntityType())
                .setEntityId(comment.getEntityId())
                .setData("postId", discussPostId);

        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            DiscussPost target = discussPostService.findDiscussPostById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        } else if (comment.getEntityType() == ENTITY_TYPE_COMMENT) {
            Comment target = commentService.findCommentById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        }

        eventProducer.fireEvent(event);

        // 触发 发帖事件将es中的数据覆盖
        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            // trigger event
            event = new Event()
                    .setTopic(TOPIC_PUBLISH)
                    .setUserId(comment.getUserId())
                    .setEntityType(ENTITY_TYPE_POST)
                    .setEntityId(discussPostId);
            eventProducer.fireEvent(event);
        }

        return "redirect:/discuss/detail/" + discussPostId;
    }

    @RequestMapping(path = "my-replys", method = RequestMethod.GET)
    @LoginRequired
    public String getMyReplys(Page page, Model model) {
        User user = hostHolder.getUser();

        int commentCount = commentService.findCommentCountByUserId(user.getId(), ENTITY_TYPE_POST);
        model.addAttribute("commentCount", commentCount);

        page.setLimit(10);
        page.setPath("/comment/my-replys");
        page.setRows(commentCount);

        List<Comment> commentList = commentService.findCommentByUserId(user.getId(), ENTITY_TYPE_POST, page.getOffset(), page.getLimit());
        List<Map<String, Object>> comments = new ArrayList<>();
        if (commentList != null) {
            for (Comment comment: commentList) {
                Map<String, Object> map = new HashMap<>();
                map.put("comment", comment);
                DiscussPost post = discussPostService.findDiscussPostById(comment.getEntityId());
                map.put("postTitle", post.getTitle());

                comments.add(map);
            }
        }

        model.addAttribute("comments", comments);

        return "/site/my-reply";
    }

}
