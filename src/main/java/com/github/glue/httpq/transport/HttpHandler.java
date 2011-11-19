/**
 * 
 */
package com.github.glue.httpq.transport;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.github.glue.httpq.model.Message;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;


/**
 * @author eric
 *
 */
public class HttpHandler extends NettyHandler {
	private Logger log = LoggerFactory.getLogger(getClass());
	/* (non-Javadoc)
	 * @see com.github.glue.httpq.transport.NettyHandler#handle(org.jboss.netty.handler.codec.http.HttpRequest)
	 */
	@Override
	protected void handle(HttpRequest request, Channel channel) {
		String uri = request.getUri();
		log.debug(uri);
		try{
		
			if(uri.startsWith("/q?") && request.getMethod() == HttpMethod.GET){
				// /q?name=user.add&clientId=123-43-12345
				QueryStringDecoder decoder = new QueryStringDecoder(uri,Charsets.UTF_8);
				List<String> clientIds = decoder.getParameters().get("clientId");
				List<String> names = decoder.getParameters().get("name");
				log.debug("clientId: {}",clientIds);
				log.debug("name: {}",names);
				if(clientIds == null || clientIds.isEmpty()){
					String clientId = UUID.randomUUID().toString();
					channel.write(Reply.as().with("{clentId: '"+clientId+"'}").toResponse());
				}else if(names != null && !names.isEmpty()){
					String clientId = clientIds.get(0);
					String name = names.get(0);
					String queueName = clientId + "#" +name;
					LinkedBlockingQueue<Message> queue = getQueue(queueName);
					if(queue == null){
						createQueue(queueName);
						queue = getQueue(queueName);
						bindQueue(name, queueName);
					}
					List<Message> pushMessages = Lists.newArrayList();
					Message message = queue.poll(10, TimeUnit.SECONDS);
					pushMessages.add(message);
					while(queue.size()>0){
						message = queue.poll(10, TimeUnit.SECONDS);
						pushMessages.add(message);
					}
					
					String json = JSON.toJSONString(pushMessages);
					log.debug("Push Json: {}", json);
					channel.write(Reply.as().with(json).toResponse());
				}
				
				
			}else if(uri.equalsIgnoreCase("/q") && request.getMethod() == HttpMethod.POST){
				// content: name=user.add
				//		    body={id=123,name=lily}
				
				byte[] bytes = readContent(request);
				String parameterString = new String(bytes);
				QueryStringDecoder decoder = new QueryStringDecoder(uri + "?" + parameterString, Charsets.UTF_8);
				
				List<String> names = decoder.getParameters().get("name");
				List<String> bodys = decoder.getParameters().get("body");
				log.debug("name: {}",names);
				log.debug("body: {}",bodys);
				
				if(names != null && !names.isEmpty() &&
						bodys != null && !bodys.isEmpty()){
					String name = names.get(0);
					String body = bodys.get(0);
					Message message = new Message();
					message.addHeader(Message.HEADER_ROUTINGKEY, name);
					message.setBody(body);
					this.route(name, message);
					channel.write(Reply.as().with("{message: 'done'}").toResponse());
				}
			}
			
			
			channel.disconnect();
		    channel.close();
		}catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/* (non-Javadoc)
	 * @see com.github.glue.httpq.transport.NettyHandler#handleException(java.lang.Throwable)
	 */
	@Override
	protected void handleException(Throwable e) {
		// TODO Auto-generated method stub

	}
	
	
	private byte[] readContent(HttpRequest request){
		byte[] content = new byte[0];
		try{
			long length = HttpHeaders.getContentLength(request);
			ChannelBuffer channelBuffer = request.getContent();
			
			ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
			long index = 0;
			while(index < length){
				int readableLength = channelBuffer.readableBytes();
				channelBuffer.readBytes(swapStream, readableLength);
				index += readableLength;
			}
			content = swapStream.toByteArray();
			swapStream.close();
		}catch (Exception e) {
			throw new RuntimeException(e);
		}
		return content;
	}

}
