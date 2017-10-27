package com.company.kafka.consumer;

import com.company.biz.constant.BizEnumConstants;
import com.company.biz.vo.OrderVo;
import com.company.kafka.ZkKafkaOffsetManager;
import com.company.websocket.server.WebSocketServer;
import com.company.websocket.service.ChannelContainer;
import com.company.websocket.util.ClientRequest;
import com.company.websocket.util.ResourceHelper;
import kafka.common.TopicAndPartition;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: BG244210
 * Date: 24/10/2017
 * Time: 14:39
 * To change this template use File | Settings | File Templates.
 * Description:
 */
@Component
public class ScanRecordProtoConsumer implements InitializingBean{
    @Autowired
    private WebSocketServer webSocketServer;

    private static final Logger LOG = LoggerFactory.getLogger(ScanRecordProtoConsumer.class.getName());

    public  void start() throws Exception {
        ResourceHelper resourceHelper = ResourceHelper.getInstance();

        String topicName = resourceHelper.getStrValue("consumer.topicName");
        Properties props = new Properties();
        props.put("bootstrap.servers",  resourceHelper.getStrValue("kafka.brokers"));
        props.put("group.id", resourceHelper.getStrValue("consumer.group.id"));
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");
        props.put("session.timeout.ms", "30000");

        //要发送自定义对象，需要指定对象的反序列化类
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<String, byte[]>(props);
        ZkKafkaOffsetManager kafkaOffsetManager = new ZkKafkaOffsetManager(resourceHelper.getStrValue("appName"));

        String readFrom = resourceHelper.getStrValue("consumer.readFrom");//smallest  largest
        TopicPartition tp;
        java.util.Map<TopicAndPartition, Object> map1;

        map1 = kafkaOffsetManager.getOffset(topicName, readFrom);
        java.util.Map initMap = new HashMap();
        java.util.Map map = new HashMap();
        map1.forEach((key, value) -> {
            initMap.put(key.partition(), value);
        });

        while (true) {
            System.out.println("**************************************************************");
            map1 = kafkaOffsetManager.getOffset(topicName, readFrom);

            boolean flag = false;
            for (java.util.Map.Entry<TopicAndPartition, Object> k : map1.entrySet()) {
                tp = new TopicPartition(k.getKey().topic(), k.getKey().partition());
                consumer.assign(Arrays.asList(new TopicPartition(k.getKey().topic(), k.getKey().partition())));
                Long offsetPerPartion = 0L;
                if (!map.isEmpty() && map.containsKey(k.getKey().partition())) {
                    offsetPerPartion = (Long) map.get(k.getKey().partition());
                } else {
                    offsetPerPartion = (Long) k.getValue();
                }
                consumer.seek(tp, (Long) offsetPerPartion);//不改变当前offset
                ConsumerRecords<String, byte[]> records = consumer.poll(100);
                flag = flag || !records.isEmpty();
                for (ConsumerRecord<String, byte[]> record : records) {

                    OrderVo.OrderDetail orderDetail = OrderVo.OrderDetail.parseFrom(record.value());
                    LOG.info("partition:[{}],  offset:[{}],  key:[{}],  " +
                            "value:(serialNo|orderName|orderPrice|address         [0])[{}]   [{}]  [{}]  [{}] ",
                            k.getKey().partition() , record.offset() ,  record.key()
                            , orderDetail.getSerialNo() , orderDetail.getOrderName(),  orderDetail.getOrderPrice(),
                            orderDetail.getAddressListOrBuilder(0).getAddress());
                    map.put(k.getKey().partition(), record.offset() + 1);


                    ChannelContainer.GLOBAL_CHANNEL_MAP.forEach((reqId, callBack) -> { // 将消息发送到所有机器
                        ClientRequest serviceRequest = new ClientRequest();
                        serviceRequest.setServiceId(BizEnumConstants.ServiceIdEnum.receive_message.code);
                        serviceRequest.setSessionId("000");
                        serviceRequest.setUserName("广播消息");
                        serviceRequest.setMessage(orderDetail.getOrderName() +"  "+ orderDetail.getOrderPrice() +"  "+
                                orderDetail.getAddressListOrBuilder(0).getAddress());
                        try {
                            callBack.send(serviceRequest);
                        } catch (Exception e) {
                            LOG.warn("回调发送消息给客户端异常", e);
                        }
                    });
                }
            }

            if (map != null && !map.isEmpty() && flag) {
                map.forEach((key, value) -> {
                    initMap.put(key, value);
                });
                kafkaOffsetManager.storeOffset(topicName, initMap);
            }
            TimeUnit.SECONDS.sleep(1);
        }

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    webSocketServer.start(ResourceHelper.getInstance().getIntValue("websocket.port"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        this.start();
    }
}
