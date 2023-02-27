package com.nowcoder.community;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class RedisTests {

    private RedisTemplate redisTemplate;

    @Autowired
    public void setRedisTemplate(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Test
    public void testStrings() {
        String redisKey = "test:count";
        redisTemplate.opsForValue().set(redisKey, 1);
        System.out.println(redisTemplate.opsForValue().get(redisKey));
        System.out.println(redisTemplate.opsForValue().increment(redisKey));
        System.out.println(redisTemplate.opsForValue().decrement(redisKey));
    }

    @Test
    public void testHashes() {
        String redisKey = "test:user";
        redisTemplate.opsForHash().put(redisKey, "id", 1);
        redisTemplate.opsForHash().put(redisKey, "name", "wang");

        System.out.println(redisTemplate.opsForHash().get(redisKey, "id"));
        System.out.println(redisTemplate.opsForHash().get(redisKey, "name"));
    }

    @Test
    public void testLists() {
        String redisKey = "test:ids";
        redisTemplate.opsForList().leftPush(redisKey, 101);
        redisTemplate.opsForList().leftPush(redisKey, 101);
        redisTemplate.opsForList().leftPush(redisKey, 101);

        System.out.println(redisTemplate.opsForList().range(redisKey, 0, 2));

        redisTemplate.delete(redisKey);
    }

    @Test
    public void testSets() {
        String redisKey = "test:teachers";
        redisTemplate.opsForSet().add(redisKey, "a", "b", "c");

        System.out.println(redisTemplate.opsForSet().members(redisKey));

        redisTemplate.delete(redisKey);
    }

    @Test
    public void testSortedSets() {
        String redisKey = "test:students";
        redisTemplate.opsForZSet().add(redisKey, "a", 30);
        redisTemplate.opsForZSet().add(redisKey, "b", 20);
        redisTemplate.opsForZSet().add(redisKey, "c", 10);

        System.out.println(redisTemplate.opsForZSet().score(redisKey, "b"));
        System.out.println(redisTemplate.opsForZSet().range(redisKey, 0, 2));

        redisTemplate.delete(redisKey);
    }

    // 绑定redisKey
    @Test
    public void testBoundOperations() {
        String redisKey = "test:count";
        BoundValueOperations operations = redisTemplate.boundValueOps(redisKey);
        operations.increment();
        operations.increment();
        operations.increment();
        System.out.println(operations.get());
    }

    // 编程式事务
    @Test
    public void testTransactional() {
        Object obj = redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String redisKey = "test:tx";

                // 启动事务
                operations.multi();

                operations.opsForSet().add(redisKey, "aa");
                operations.opsForSet().add(redisKey, "bb");
                operations.opsForSet().add(redisKey, "cc");

                // 返回空，因为事务会在最终一起提交
                // 因此不要在事务中进行查询，无效
                System.out.println(operations.opsForSet().members(redisKey));

                return operations.exec(); // 返回 [1, 1, 1, [cc, aa, bb]]。前者是每次操作影响的行数

            }
        });

        System.out.println(obj);
    }

}
