package nikheel.fcs.route;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http.HttpMethods;
import org.springframework.stereotype.Component;

@Component
public class RestApiRoute extends RouteBuilder {

    @Override
    public void configure() {
        from("direct:getUser")
        .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.GET))
        .toD("https://jsonplaceholder.typicode.com/users/1")
        .log("Response: ${body}")
            .convertBodyTo(String.class);
    }
}
