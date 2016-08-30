package com.demo.http;

import io.netty.channel.ChannelInitializer;  
import io.netty.channel.ChannelPipeline;  
import io.netty.channel.socket.SocketChannel;  
import io.netty.example.securechat.SecureChatSslContextFactory;  
import io.netty.handler.codec.http.HttpContentCompressor;  
import io.netty.handler.codec.http.HttpRequestDecoder;  
import io.netty.handler.codec.http.HttpResponseEncoder;  
import io.netty.handler.ssl.SslHandler;  
  
  
  
import javax.net.ssl.SSLEngine;  
  
public class HttpDemoServerInitializer extends ChannelInitializer<SocketChannel> {  
    @Override  
    public void initChannel(SocketChannel ch) throws Exception {  
        // Create a default pipeline implementation.  
        ChannelPipeline pipeline = ch.pipeline();  
  
        if (HttpDemoServer.isSSL) {  
        	
            SSLEngine engine = SecureChatSslContextFactory.getServerContext().createSSLEngine();  
            engine.setUseClientMode(false);  
            pipeline.addLast("ssl", new SslHandler(engine));  
        }  
  
        /** 
         * http-request解码器 
         * http服务器端对request解码 
         */  
        pipeline.addLast("decoder", new HttpRequestDecoder());  
        /** 
         * http-response解码器 
         * http服务器端对response编码 
         */  
        pipeline.addLast("encoder", new HttpResponseEncoder());  
  
        /** 
         * 压缩 
         * Compresses an HttpMessage and an HttpContent in gzip or deflate encoding 
         * while respecting the "Accept-Encoding" header. 
         * If there is no matching encoding, no compression is done. 
         */  
        pipeline.addLast("deflater", new HttpContentCompressor());  
  
        pipeline.addLast("handler", new HttpDemoServerHandler());  
    }  
} 