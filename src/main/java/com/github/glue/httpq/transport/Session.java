/**
 * 
 */
package com.github.glue.httpq.transport;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.github.glue.httpq.model.Exchanger;
import com.github.glue.httpq.model.Message;
import com.google.common.collect.Lists;

/**
 * @author eric
 *
 */
public class Session {

	String sid;
	Exchanger exchanger;
	int expiry;
	
	public Session(String sid, Exchanger exchanger, int expiry) {
		super();
		this.sid = sid;
		this.exchanger = exchanger;
		this.expiry = expiry;
	}



	public Consumer getConsumer(){
		BlockingQueue<Message> queue = exchanger.getQueue(sid);
		if(queue == null){
			throw new RuntimeException("Can't find consumer queue, do you bind exchange?");
		}
		return new Consumer(queue);
	}
	
	public Producer getProducer(){
		return new Producer();
	}
	
	public void bind(String exchangeName,String type){
		
		BlockingQueue<Message> queue = exchanger.getQueue(sid);
		if(queue == null){
			exchanger.createQueue(sid);
		}
		
		if(!exchanger.checkBind(exchangeName, sid)){
			exchanger.bindQueue(exchangeName, sid);
		}
	}
	
	
	public class Consumer{
		BlockingQueue<Message> queue;

		public Consumer(BlockingQueue<Message> queue) {
			super();
			this.queue = queue;
		}
		
		public Message[] getMessages() throws InterruptedException{
			List<Message> messages = Lists.newArrayList();
			Message message = queue.poll(60, TimeUnit.SECONDS);
			if(message != null){
				messages.add(message);
			}
			queue.drainTo(messages);
			return messages.toArray(new Message[messages.size()]);
		}
	}
	
	public class Producer{
		public void deliver(String name, String type, Message message){
			exchanger.route(name, message);
		}
	}
	
}
