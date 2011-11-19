/**
 * 
 */
package com.github.glue.httpq.model;

import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * @author eric
 *
 */
public class ExchangeHandler {
	private Logger log = LoggerFactory.getLogger(getClass());
	final static String DIRECT = "amq.direct";
	final static String TOPIC = "amq.topic";
	
	//一个客户端对应一个队列,key为cid
	Map<String, LinkedBlockingQueue<Message>> queues = Maps.newConcurrentMap();
	//一个exchage名对应多个
	Map<String, List<LinkedBlockingQueue<Message>>> exchages = Maps.newConcurrentMap();
	
	public void createQueue(String name){
		queues.put(name, new LinkedBlockingQueue<Message>());
	}
	
	public LinkedBlockingQueue<Message> getQueue(String name){
		return queues.get(name);
	}
	
	public void bindQueue(String bindingKey, String queueName){
		List<LinkedBlockingQueue<Message>> queueList = exchages.get(bindingKey);
		if(queueList == null){
			queueList = Lists.newArrayList();
			exchages.put(bindingKey, queueList);
		}
		LinkedBlockingQueue<Message> queue = getQueue(queueName);
		if(queue != null){
			queueList.add(queue);
		}
	}
	
	public void route(String routingKey, Message message) {
		List<LinkedBlockingQueue<Message>> queueList = exchages.get(routingKey);
		if(queueList != null){
			for (LinkedBlockingQueue<Message> queue : queueList) {
				try {
					queue.put(message);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}
		log.debug("routeing key: {}, message: {}", routingKey,message);
	}
}
