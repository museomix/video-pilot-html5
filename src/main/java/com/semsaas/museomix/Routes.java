package com.semsaas.museomix;

import java.io.File;
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

	String device = "/dev/ttyACM0";
	String OS = System.getProperty("os.name").toLowerCase();

	@Override
	public void configure() throws Exception {
		String configDevice = System.getProperty("device");
		if(configDevice != null) {
			device = configDevice;
		}
		
		eventQueue = this.getContext().getEndpoint("seda:events");
		eventConsumer = eventQueue.createPollingConsumer();
		if(OS.indexOf("mac") >= 0) {
			Runtime.getRuntime().exec("/bin/stty -f "+device+" 115200");			
		} else if (OS.indexOf("nux") >= 0) {
			Runtime.getRuntime().exec("/bin/stty -F "+device+" 115200");
		} else {
			throw new Exception("Unsupported OS "+OS);
		}
		if(!new File(device).exists()) {
			throw new Exception("Device not found "+device);
		}
		
		from("servlet://api/pad/state")
			.process(consumePad)
			.to("log:raw")
			.convertBodyTo(String.class)
		;
		
		from("stream:file?fileName="+device+"&scanStream=true")
			.to("log:raw")
			.to("seda:events")
		;
	}

}
