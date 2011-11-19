/**
 * 
 */
package com.github.glue.httpq.transport;

import java.util.Map;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.DynamicChannelBuffer;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;


/**
 * @author eric
 *
 */
public class Reply {
	
	String contentType = "text/json;charset=UTF-8";
	Map<String, String> headers;
	String content;
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

	Reply with(String content) {
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
		
		if(!Strings.isNullOrEmpty(content)){
			byte[] body = content.getBytes();
			ChannelBuffer buffer = new DynamicChannelBuffer(body.length);
			buffer.writeBytes(body);
			response.setContent(buffer);
		}
		
		return response;
	}

	
//	public static Reply asXml(){
//		return null;
//	}
}
