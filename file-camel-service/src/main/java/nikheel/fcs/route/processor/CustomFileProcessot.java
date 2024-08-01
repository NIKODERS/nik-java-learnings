package nikheel.fcs.route.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component(value = "CaseChangeProcessor")
public class CustomFileProcessot implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		String originalBody = exchange.getIn().getBody(String.class);
		String processedBody = originalBody.toUpperCase();
		exchange.getIn().setBody(processedBody);
	}
}
