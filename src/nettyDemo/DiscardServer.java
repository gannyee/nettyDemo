package nettyDemo;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * Discard any incoming data
 * @author ganyi
 *
 */
public class DiscardServer {
	private int port;
	
	public DiscardServer(int port){
		this.port = port;
	}
	
	public void run() throws InterruptedException{
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		ServerBootstrap b = new ServerBootstrap();
		b.group(bossGroup,workerGroup)
		.channel(NioServerSocketChannel.class)
		.childHandler(new ChannelInitializer<SocketChannel>() {

			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				// TODO Auto-generated method stub
				ch.pipeline().addLast(new DiscardServerHandler());
			}
		})
		.option(ChannelOption.SO_BACKLOG,128)
		.childOption(ChannelOption.SO_KEEPALIVE,true);
		
		//Bind and start to accept incoming connection
		ChannelFuture f = b.bind(port).sync();
		
		//Wait until the server socket is close
		f.channel().closeFuture().sync();
	}
	
	public static void main(String[] args) throws InterruptedException {
		int port;
		if(args.length > 0){
			port = Integer.parseInt(args[0]);
		}else{
			port = 8080;
		}
		new DiscardServer(port).run();
	}
}
