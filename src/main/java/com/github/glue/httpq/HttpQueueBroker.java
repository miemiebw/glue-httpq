package com.github.glue.httpq;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

import com.github.glue.httpq.model.ExchangeManager;
import com.github.glue.httpq.transport.HttpHandler;

public class HttpQueueBroker {
	private Logger log = LoggerFactory.getLogger(getClass());
	ExecutorService executor;
	ChannelFactory channelFactory;
	Channel channel;
	ExchangeManager exchangeManager;
	String host;
	int port;
	public HttpQueueBroker(String host, int port){
		this.host = host;
		this.port = port;
	}
	
	public void start(){
		executor = Executors.newCachedThreadPool();
		channelFactory = new NioServerSocketChannelFactory(executor,executor);
		exchangeManager = new ExchangeManager();
		
		ChannelPipelineFactory pipelineFactory = new ChannelPipelineFactory(){

			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline pipeline = Channels.pipeline();
				pipeline.addLast("decoder", new HttpRequestDecoder());
		        pipeline.addLast("encoder", new HttpResponseEncoder());
		        pipeline.addLast("http", new HttpHandler(exchangeManager));
				return pipeline;
			}
		};
		InetSocketAddress address = new InetSocketAddress(host,port);
		channel = makeAcceptor(channelFactory, pipelineFactory, address);
		log.info("start...");
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
		HttpQueueBroker broker = new HttpQueueBroker("", 6360);
		broker.start();
	}
}
