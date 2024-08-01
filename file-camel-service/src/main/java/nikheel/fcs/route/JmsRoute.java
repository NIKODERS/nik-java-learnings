package nikheel.fcs.route;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class JmsRoute extends RouteBuilder {

    @Override
    public void configure() {
        from("jms:queue:inputQueue")
            .unmarshal().json() // Convert JSON to Map
            .choice()
                .when(simple("${body[status]} == 'new'"))
                    .marshal().jacksonXml() // Convert to XML
                    .to("jms:queue:newOrdersQueue")
                .when(simple("${body[status]} == 'processed'"))
                    .marshal().jacksonXml()
                    .to("jms:queue:processedOrdersQueue")
                .otherwise()
                    .marshal().jacksonXml()
                    .to("jms:queue:otherOrdersQueue");
    }
}
