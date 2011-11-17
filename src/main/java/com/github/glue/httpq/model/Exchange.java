/**
 * 
 */
package com.github.glue.httpq.model;

import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.common.collect.Maps;

/**
 * @author eric
 *
 */
public class Exchange {
	public final static String DIRECT = "amq.direct";
	public final static String TOPIC = "amq.topic";
	
	Map<String, LinkedBlockingQueue<Message>> queues = Maps.newConcurrentMap();
	Map<String, List<String>> exchages = Maps.newConcurrentMap();
	
	public void createQueue(String name){
		queues.put(name, new LinkedBlockingQueue<Message>());
	}
	
	public LinkedBlockingQueue<Message> getQueue(String name){
		return queues.get(name);
	}
	
	public void bindQueue(String bindingKey, String queueName){
		
	}
	
	public void route(String routingKey, Message message){
		
	}
}
