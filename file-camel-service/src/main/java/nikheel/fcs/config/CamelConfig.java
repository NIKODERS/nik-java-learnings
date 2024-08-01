package nikheel.fcs.config;
import org.apache.camel.CamelContext;
import org.apache.camel.component.jms.JmsComponent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.jms.ConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
@Configuration
public class CamelConfig {

    @Value("${activemq.broker-url}")
    private String brokerUrl;
    @Bean
    public ConnectionFactory connectionFactory() {
        return (ConnectionFactory) new ActiveMQConnectionFactory(brokerUrl);
    }

    @Bean
    public JmsComponent jmsComponent(CamelContext context, ActiveMQConnectionFactory connectionFactory) {
        JmsComponent jmsComponent = JmsComponent.jmsComponentAutoAcknowledge(connectionFactory);
        context.addComponent("jms", jmsComponent);
        return jmsComponent;
    }
}
