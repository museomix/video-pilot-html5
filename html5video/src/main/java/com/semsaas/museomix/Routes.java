package com.semsaas.museomix;

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
		public void process(Exchange exchange) throws Exception {
			Message in = exchange.getIn();
			Exchange evtExchange = eventConsumer.receive();
			
			Message evtIn = evtExchange.getIn();
			in.setBody(evtIn.getBody());
		}
	};
	
	@Override
	public void configure() throws Exception {		
		eventQueue = this.getContext().getEndpoint("seda:events");
		eventConsumer = eventQueue.createPollingConsumer();
		
		from("servlet://api/pad/state")
			.process(consumePad)
		;
		
		from("stream:file?fileName=/dev/ttyACM0&scanStream=true")
			.to("seda:events")
		;
	}

}
