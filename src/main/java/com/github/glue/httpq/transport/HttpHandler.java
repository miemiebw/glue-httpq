/**
 * 
 */
package com.github.glue.httpq.transport;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.github.glue.httpq.Context;
import com.github.glue.httpq.model.Message;
import com.github.glue.httpq.transport.Session.Consumer;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;


/**
 * @author eric
 *
 */
public class HttpHandler extends NettyHandler {

	private Logger log = LoggerFactory.getLogger(getClass());
	Context context;
	
	public HttpHandler(Context context) {
		super();
		this.context = context;
	}


	/* (non-Javadoc)
	 * @see com.github.glue.httpq.transport.NettyHandler#handle(org.jboss.netty.handler.codec.http.HttpRequest)
	 */
	@Override
	protected void handle(HttpRequest request) {
		String uri = request.getUri();
		log.debug(uri);
		try{
		
			if(uri.startsWith("/q?") && request.getMethod() == HttpMethod.GET){
				// /q?name=user.add&clientId=123-43-12345
				QueryStringDecoder decoder = new QueryStringDecoder(uri,Charsets.UTF_8);
				List<String> clientIds = decoder.getParameters().get("clientId");
				List<String> names = decoder.getParameters().get("name");
				if(clientIds == null || clientIds.isEmpty() || Strings.isNullOrEmpty(clientIds.get(0))){
					getClient();
				}else if(names != null && !names.isEmpty()){
					getMessage(clientIds.get(0), names.toArray(new String[names.size()]));
				}
				
				
			}else if(uri.equalsIgnoreCase("/q") && request.getMethod() == HttpMethod.POST){
				// content: name=user.add
				//		    body={id=123,name=lily}
				
				byte[] bytes = readContent(request);
				String parameterString = new String(bytes);
				QueryStringDecoder decoder = new QueryStringDecoder(uri + "?" + parameterString, Charsets.UTF_8);
				List<String> names = decoder.getParameters().get("name");
				List<String> bodys = decoder.getParameters().get("body");
				if(names != null && !names.isEmpty() &&
						bodys != null && !bodys.isEmpty()){
					String body = bodys.get(0);
					Session ssn = context.getSession("i have a big mouth!");
					for (String name : names) {
						Message message = new Message();
						message.addHeader(Message.HEADER_ROUTINGKEY, name);
						message.setBody(body);
						ssn.getProducer().deliver(name, "direct", message);
					}
					
					channel.write(Reply.as().with("{status:'success', op:'postMessage' ,result: 'push done.'}").type(Reply.CONTENTTYPE_JSON).toResponse());
				}
			}else{
				InputStream input = this.getClass().getResourceAsStream(uri);
				channel.write(Reply.as().with(input).type(Reply.CONTENTTYPE_HTML).toResponse());
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
		log.error("exception", e);
	}
	
	
	void getClient(){
		String clientId = UUID.randomUUID().toString();
		channel.write(Reply.as().with("{status:'success',op:'getClientId' ,result: '"+clientId+"'}").type(Reply.CONTENTTYPE_JSON).toResponse());
	}
	
	
	void getMessage(String clientId,String[] names) throws InterruptedException{
		Session ssn = context.getSession(clientId);
		for (String name : names) {
			ssn.bind(name, "direct");
		}

		Consumer consumer = ssn.getConsumer();
	
		StringBuilder builder = new StringBuilder("{status:'success', op:'getMessage', clientId:'");
		builder.append(clientId);
		builder.append("' ,result:");
		builder.append(JSON.toJSON(consumer.getMessages()));
		builder.append("}");
		
		channel.write(Reply.as().with(builder.toString()).type(Reply.CONTENTTYPE_JSON).toResponse());
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
