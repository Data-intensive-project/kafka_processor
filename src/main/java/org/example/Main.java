package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.metrics.stats.Value;

import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;

import java.security.Key;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;

public class Main {

    public static final String INTEGER_TOPIC_NAME = "test20";

    static KafkaProducer<Integer,String> producer;
    public static Properties createProperties() {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "even-odd-branch");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.Integer().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        return props;
    }
    public static Message convertStringToMessage(String messageJson){
        ObjectMapper mapper = new ObjectMapper();
        Message message = null;
        try {
            message = mapper.readValue(messageJson, Message.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return message;
    }
    public static Topology createTopology() {
        final StreamsBuilder builder = new StreamsBuilder();
        KStream<Integer, String> stream = builder.stream(INTEGER_TOPIC_NAME);
        stream.foreach((key, value)-> {
            
            Message message = convertStringToMessage(value);
            if (message.getWeather().getHumidity()>70){
                ProducerRecord<Integer, String> record = new ProducerRecord<>("topic4",2,
                        "humidity over 70");
                    producer.send(record);

                System.out.println("over 70");
            }
                ProducerRecord<Integer, String> record = new ProducerRecord<>("test21",2,
                        value);
                producer.send(record);

        });

        return builder.build();
    }
    public static void main(String[]args){
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Properties props = createProperties();

        final Topology topology = createTopology();
        final KafkaStreams streams = new KafkaStreams(topology, props);
        final CountDownLatch latch = new CountDownLatch(1);

        Properties props1 = new Properties();
        props1.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props1.put(ProducerConfig.ACKS_CONFIG, "all");
        props1.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
        props1.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producer = new KafkaProducer<>(props1);

        Runtime.getRuntime().addShutdownHook(new Thread("z") {
            @Override
            public void run() {
                streams.close();
                latch.countDown();
            }
        });

        try {
            streams.start();
            latch.await();
        } catch (Throwable e) {
            System.exit(1);
        }
        System.exit(0);
    }
}
