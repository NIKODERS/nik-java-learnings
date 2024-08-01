package nikheel.fcs.route;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FileMoverRoute extends RouteBuilder {
	@Autowired
	private Processor customProcessor;

	@Override
	public void configure() throws Exception {
		from("file:C://inputFolder?noop=true").
		log(LoggingLevel.INFO,"Received input file as ${body}").
		process(customProcessor)		
		.to("file:C://outputFolder").
		log(LoggingLevel.INFO,"Received input file as ${body}");
	}
}