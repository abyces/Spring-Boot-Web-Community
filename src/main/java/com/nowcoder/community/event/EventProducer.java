package com.nowcoder.community.event;

import com.alibaba.fastjson2.JSONObject;
import com.nowcoder.community.entity.Event;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventProducer {

    private KafkaTemplate kafkaTemplate;

    public EventProducer(KafkaTemplate kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // 处理事件（发布消息）
    public void fireEvent(Event event) {
        String json = JSONObject.toJSONString(event);
        kafkaTemplate.send(event.getTopic(), JSONObject.toJSONString(event));
    }
}
