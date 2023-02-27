package com.nowcoder.community.service;

import com.nowcoder.community.dao.elasticsearch.DiscussPostRepository;
import com.nowcoder.community.entity.DiscussPost;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ElasticsearchService {

    private DiscussPostRepository discussPostRepository;
    private ElasticsearchOperations elasticsearchOperations;

    public ElasticsearchService(DiscussPostRepository discussPostRepository, ElasticsearchOperations elasticsearchOperations) {
        this.discussPostRepository = discussPostRepository;
        this.elasticsearchOperations = elasticsearchOperations;
    }

    public void saveDiscussPost(DiscussPost discussPost) {
        discussPostRepository.save(discussPost);
    }

    public void deleteDiscussPost(int id) {
        discussPostRepository.deleteById(id);
    }

    public List<SearchHit<DiscussPost>> searchDiscussPost(String keyword, int page, int pageSize) {
        return discussPostRepository.findDiscussPostByTitleContainingOrContentContainingOrderByTypeDescScoreDescCreateTimeDesc(keyword, keyword, PageRequest.of(page, pageSize));
    }


    /**
     * 如果在Query中增加Sorting和HighlighFields，则可以直接代替Repository中的方法
     * 问题在于Repository Method无法返回SearchHits，从而取不到TotalHit （或者我没找到）
     */
    public int getSearchCount(String keyword) {
        Query query = NativeQuery.builder()
                .withQuery(q -> q
                        .match(m -> m
                                .field("title")
                                .query(keyword)
                        )
                )
                .withQuery(q -> q
                        .match(m -> m
                                .field("content")
                                .query(keyword)))
                .withPageable(PageRequest.of(1, 1))
                .build();

        SearchHits<DiscussPost> res = elasticsearchOperations.search(query, DiscussPost.class);
        return (int) res.getTotalHits();
    }
}
