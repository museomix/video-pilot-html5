package com.semsaas.museomix;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.PollingConsumer;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

public class Routes extends RouteBuilder {
	Endpoint eventQueue;
	PollingConsumer eventConsumer;

	Processor consumePad = new Processor() {
		Pattern padEventPattern = Pattern.compile(">G.*Gesture = ([A-Z]*)$");
		public void process(Exchange exchange) throws Exception {
			Message in = exchange.getIn();
			String event = null;
			while(event == null) {
				Exchange evtExchange = eventConsumer.receive();
				Message evtIn = evtExchange.getIn();
				event = evtIn.getBody(String.class);			
				Matcher matcher = padEventPattern.matcher(event);
				if(matcher.matches()) {
					event = matcher.group(1);
				} else {
					event = null;
				}
			}
			
			in.setBody(event);
		}
	};
	
	@Override
	public void configure() throws Exception {		
		eventQueue = this.getContext().getEndpoint("seda:events");
		eventConsumer = eventQueue.createPollingConsumer();
		
		from("servlet://api/pad/state")
			.process(consumePad)
			.to("log:raw")
			.convertBodyTo(String.class)
		;
		
		from("stream:file?fileName=/dev/ttyACM0&scanStream=true")
			.to("log:raw")
			.to("seda:events")
		;
	}

}
