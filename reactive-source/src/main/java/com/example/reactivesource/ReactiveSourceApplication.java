package com.example.reactivesource;

import org.apache.activemq.artemis.jms.client.ActiveMQQueue;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.jms.dsl.Jms;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import javax.jms.ConnectionFactory;

@RestController
@SpringBootApplication
public class ReactiveSourceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReactiveSourceApplication.class, args);
    }

    @Autowired
    private ConnectionFactory connectionFactory;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Bean
    public Publisher<Message<String>> jmsReactiveSource() {
        return IntegrationFlows
                .from(Jms.messageDrivenChannelAdapter(this.connectionFactory)
                        .destination(new ActiveMQQueue("testQueue")))
                .channel(MessageChannels.queue())
                .log(LoggingHandler.Level.INFO)
                .log()
                .toReactivePublisher();
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> getPatientAlerts() {
        return Flux.from(jmsReactiveSource())
                .map(Message::getPayload);
    }

    @GetMapping(value = "/generate")
    public void generateJmsMessage() {
        for (int i = 0; i < 10; i++) {
            this.jmsTemplate.convertAndSend("testQueue", "testMessage #" + (i + 1));
        }
    }
}
