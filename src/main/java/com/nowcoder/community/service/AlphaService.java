package com.nowcoder.community.service;

import com.nowcoder.community.dao.AlphaDao;
import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Date;

@Service
public class AlphaService {

    private AlphaDao alphaDao;
    private UserMapper userMapper;
    private DiscussPostMapper discussPostMapper;
    private TransactionTemplate transactionTemplate;

    public AlphaService() {
        System.out.println("construct AlphaService");
    }
    @PostConstruct
    public void init() {
        System.out.println("init AlphaService");
    }

    @PreDestroy
    public void destroy() {
        System.out.println("destroy AlphaService");
    }

    @Autowired
    public void setAlphaDao(AlphaDao alphaDao){
        this.alphaDao = alphaDao;
    }

    @Autowired
    public void setUserMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Autowired
    public void setDiscussPostMapper(DiscussPostMapper discussPostMapper) {
        this.discussPostMapper = discussPostMapper;
    }

    @Autowired
    public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    public String find() {
        return alphaDao.select();
    }

    // ????????????Propagation ??????A ?????? ??????B???????????????B??????????????????????????????
    // REQUIRED ??????????????????????????????????????????????????????????????????????????????
    // REQUIRES_NEW ????????????????????????????????????????????????
    // NESTED ???????????????????????????????????????????????????????????????????????????????????????????????????REQUIRED??????
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public Object save1() {
        // add user
        User user = new User();
        user.setUsername("alpha");
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
        user.setPassword(CommunityUtil.md5("123" + user.getSalt()));
        user.setEmail("testalpha@qq.com");
        user.setHeaderUrl("http://image.nowcoder.com/head/99t.com");
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        // add post
        DiscussPost post = new DiscussPost();
        post.setUserId(user.getId()); // insert into User (<include refid="insertFields"></include>). ?????????user-mapper.xml??????????????????MyBatis?????????????????????userId?????????????????????
        post.setTitle("Hellooo");
        post.setContent("??????");
        post.setCreateTime(new Date());
        discussPostMapper.insertDiscussPost(post);

        // create an error to test rollback
        Integer.valueOf("abc");

        return "ok";
    }

    public Object save2() {
        transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

        return transactionTemplate.execute(new TransactionCallback<Object>() {
            @Override
            public Object doInTransaction(TransactionStatus status) {
                // add user
                User user = new User();
                user.setUsername("beta");
                user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
                user.setPassword(CommunityUtil.md5("123" + user.getSalt()));
                user.setEmail("testbeta@qq.com");
                user.setHeaderUrl("http://image.nowcoder.com/head/999.com");
                user.setCreateTime(new Date());
                userMapper.insertUser(user);

                // add post
                DiscussPost post = new DiscussPost();
                post.setUserId(user.getId()); // insert into User (<include refid="insertFields"></include>). ?????????user-mapper.xml??????????????????MyBatis?????????????????????userId?????????????????????
                post.setTitle("Hellooo2");
                post.setContent("??????2");
                post.setCreateTime(new Date());
                discussPostMapper.insertDiscussPost(post);

                // create an error to test rollback
                Integer.valueOf("abc");

                return "ok";
            }
        });
    }


}
