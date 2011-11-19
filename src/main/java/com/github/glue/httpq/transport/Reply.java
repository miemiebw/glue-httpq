/**
 * 
 */
package com.github.glue.httpq.transport;

import java.io.InputStream;
import java.util.Map;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.DynamicChannelBuffer;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;


/**
 * @author eric
 *
 */
public class Reply {
	private Logger log = LoggerFactory.getLogger(getClass());
	public static final String CONTENTTYPE_JSON = "text/json;charset=UTF-8";
	public static final String CONTENTTYPE_HTML = "text/html;charset=UTF-8";
	String contentType = CONTENTTYPE_HTML;
	Map<String, String> headers;
	Object content;
	HttpResponseStatus status = HttpResponseStatus.OK;
	
	private Reply(){
		headers = Maps.newHashMap();
		headers.put("Access-Control-Allow-Origin", "*");
	    headers.put("Pragma","No-cache"); 
		headers.put("Cache-Control","no-cache"); 
		headers.put("Expires", "-1"); 
	}
	
	public Reply type(String contentType) {
		this.contentType = contentType;
		return this;
	}

	public Reply headers(Map<String, String> headers) {
		this.headers = headers;
		return this;
	}
	
	public Reply header(String name, String value){
		if(headers == null){
			headers = Maps.newHashMap();
		}
		headers.put(name, value);
		return this;
	}

	public Reply with(Object content) {
		this.content = content;
		return this;
	}

	public Reply status(HttpResponseStatus code) {
		this.status = code;
		return this;
	}

	public static Reply as(){
		return new Reply();
	}
	
	public HttpResponse toResponse(){
		
		DefaultHttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);
		response.setHeader("Content-Type", contentType);
		if(headers != null){
			for (Map.Entry<String, String> entry : headers.entrySet()) {
				response.setHeader(entry.getKey(), entry.getValue());
			}
		}
		try{
		if(content instanceof String && !Strings.isNullOrEmpty((String)content)){
			log.debug("write text: {}", content);
			byte[] body = ((String)content).getBytes();
			ChannelBuffer buffer = new DynamicChannelBuffer(body.length);
			buffer.writeBytes(body);
			response.setContent(buffer);
		}else if(content instanceof InputStream){
			InputStream input = (InputStream)content;
			ChannelBuffer buffer = new DynamicChannelBuffer(input.available());
		    buffer.writeBytes(input,input.available());
		    response.setContent(buffer);
		}
		}catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		return response;
	}

	
//	public static Reply asXml(){
//		return null;
//	}
}
