/**
 * 
 */
package com.github.glue.httpq.model;

import java.util.Map;

import com.google.common.collect.Maps;

/**
 * @author eric
 *
 */
public class Message {
	public static final String HEADER_ROUTINGKEY = "header.routingKey";
	
	Map<String, String> headers = Maps.newHashMap();
	String body;
	
	public Message() {
	}
	
	public void addHeader(String key, String value){
		headers.put(key, value);
	}
	
	public void setBody(String body){
		this.body = body;
	}
	
	

	public String getBody() {
		return body;
	}

	

	public Map<String, String> getHeaders() {
		return headers;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Message [headers=");
		builder.append(headers);
		builder.append(", body=");
		builder.append(body);
		builder.append("]");
		return builder.toString();
	}
	
	
}
