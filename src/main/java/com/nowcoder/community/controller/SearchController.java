package com.nowcoder.community.controller;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SearchController implements CommunityConstant {

    private ElasticsearchService elasticsearchService;
    private UserService userService;
    private LikeService likeService;

    public SearchController(ElasticsearchService elasticsearchService, UserService userService, LikeService likeService) {
        this.elasticsearchService = elasticsearchService;
        this.userService = userService;
        this.likeService = likeService;
    }

    // 因为是get请求，keyword用路径 search?keyword=xxx
    @RequestMapping(path = "/search", method = RequestMethod.GET)
    public String search(String keyword, Model model, Page page) {
        List<SearchHit<DiscussPost>> searchResult = elasticsearchService.searchDiscussPost(keyword, page.getCurrent() - 1, page.getLimit());
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        if (searchResult != null && !searchResult.isEmpty()) {
            for (SearchHit<DiscussPost> hit: searchResult) {
                Map<String, Object> map = new HashMap<>();

                DiscussPost post = hit.getContent();

                String highlightTitle = hit.getHighlightField("title").toString();
                if (checkHighlightField(highlightTitle))
                    post.setTitle(highlightTitle.substring(1, highlightTitle.length()-1));

                String highlightContent = hit.getHighlightField("content").toString();
                if (checkHighlightField(highlightContent))
                    post.setContent(highlightContent.substring(1, highlightContent.length()-1));

                map.put("post", post);
                map.put("user", userService.findUserById(post.getUserId()));
                map.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId()));

                discussPosts.add(map);
            }
        }

        model.addAttribute("discussPosts", discussPosts);
        model.addAttribute("keyword", keyword);

        page.setPath("/search?keyword=" + keyword);
        page.setRows(searchResult == null? 0 : (page.getRows() == 0 ? elasticsearchService.getSearchCount(keyword) : page.getRows()));

        return "/site/search";

    }

    private boolean checkHighlightField(String source) {
        return source != null && source.length() > 2 && source.charAt(0) == '[' && source.charAt(source.length()-1) == ']';
    }
}
