package com.github.glue.httpq;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.glue.httpq.model.Exchanger;
import com.github.glue.httpq.transport.HttpHandler;
import com.github.glue.httpq.transport.Session;
import com.google.common.collect.Maps;

public class HttpQueueBroker implements Context{
	private Logger log = LoggerFactory.getLogger(getClass());
	ExecutorService executor;
	ChannelFactory channelFactory;
	Channel channel;
	Exchanger exchanger;
	String host;
	int port;
	
	ScheduledExecutorService scheduExec = Executors.newScheduledThreadPool(1);  
	
	Map<String, Session> sessions = Maps.newHashMap();
	
	public HttpQueueBroker(String host, int port){
		this.host = host;
		this.port = port;
	}
	
	public void start(){
		executor = Executors.newCachedThreadPool();
		channelFactory = new NioServerSocketChannelFactory(executor,executor,100);
		exchanger = new Exchanger();
		
		ChannelPipelineFactory pipelineFactory = new ChannelPipelineFactory(){

			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline pipeline = Channels.pipeline();
				pipeline.addLast("decoder", new HttpRequestDecoder());
		        pipeline.addLast("encoder", new HttpResponseEncoder());
		        pipeline.addLast("http", new HttpHandler(HttpQueueBroker.this));
				return pipeline;
			}
		};
		InetSocketAddress address = new InetSocketAddress(host,port);
		channel = makeAcceptor(channelFactory, pipelineFactory, address);
		log.info("start...");
		scheduExec.scheduleWithFixedDelay(new CheckTask(), 10, 1, TimeUnit.SECONDS);
	}
	
	private Channel makeAcceptor(ChannelFactory channelFactory, ChannelPipelineFactory pipelineFactory,
			InetSocketAddress address){
		ServerBootstrap bootstrap = new ServerBootstrap(channelFactory);
	    bootstrap.setPipelineFactory(pipelineFactory);
	    bootstrap.setOption("backlog", 1000);
	    bootstrap.setOption("reuseAddress", true);
	    bootstrap.setOption("child.keepAlive", true);
	    bootstrap.setOption("child.tcpNoDelay", true);
	    bootstrap.setOption("child.receiveBufferSize", 2048);
	    return bootstrap.bind(address);
	}

	public static void main(String[] args){
		HttpQueueBroker broker = new HttpQueueBroker("0.0.0.0", 6360);
		broker.start();
	}

	public Session getSession(String sid) {
		Session ssn = sessions.get(sid);
		if(ssn == null){
			ssn = new Session(sid, exchanger, 10000);
			sessions.put(sid, ssn);
			log.info("Session '{}' created.",ssn.getSid());
		}
		return ssn;
	}
	
	public void removeSession(Session ssn){
		sessions.remove(ssn.getSid());
		exchanger.distroyQueue(ssn.getSid());
		log.info("Session '{}' removed.",ssn.getSid());
	}
	
	private class CheckTask implements Runnable{

		public void run() {
			for (Session ssn : sessions.values()) {
				if(ssn.isTimeout()){
					removeSession(ssn);
				}
			}
		}
		
	}
}
