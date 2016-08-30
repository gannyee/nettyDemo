package com.web;

import io.netty.bootstrap.ServerBootstrap;

public class HttpServer {

	private final int port;
	
	public HttpServer(int port){
		this.port = port;
	}
	
	public void run(){
		ServerBootstrap bootstrap = new ServerBootstrap();
	}
}
