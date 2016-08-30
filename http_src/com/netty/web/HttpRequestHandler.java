package com.netty.web;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.CookieEncoder;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.CharsetUtil;

public class HttpRequestHandler extends SimpleChannelUpstreamHandler {

	private HttpRequest request;
	private boolean readingChunks;

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		if (!readingChunks) {
			HttpRequest request = this.request = (HttpRequest) e.getMessage();
			String uri = request.getUri();
			System.out.println("-----------------------------------------------------------------");
			System.out.println("uri:"+uri);
			System.out.println("-----------------------------------------------------------------");
			/**
			 * 100 Continue
			 * ��������һ�������HTTP�ͻ��˳�����һ��ʵ������岿��Ҫ���͸�����������ϣ���ڷ���֮ǰ�鿴�·������Ƿ��
			 * �������ʵ�壬�����ڷ���ʵ��֮ǰ�ȷ�����һ��Я��100
			 * Continue��Expect�����ײ������󡣷��������յ������������Ӧ���� 100 Continue��һ����������������Ӧ��
			 */
			if (is100ContinueExpected(request)) {
				send100Continue(e);
			}
			// ����httpͷ��
			for (Map.Entry<String, String> h : request.getHeaders()) {
				System.out.println("HEADER: " + h.getKey() + " = " + h.getValue() + "\r\n");
			}
			// �����������
			QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
			Map<String, List<String>> params = queryStringDecoder.getParameters();
			if (!params.isEmpty()) {
				for (Entry<String, List<String>> p : params.entrySet()) {
					String key = p.getKey();
					List<String> vals = p.getValue();
					for (String val : vals) {
						System.out.println("PARAM: " + key + " = " + val + "\r\n");
					}
				}
			}
			if (request.isChunked()) {
				readingChunks = true;
			} else {
				ChannelBuffer content = request.getContent();
				if (content.readable()) {
					System.out.println(content.toString(CharsetUtil.UTF_8));
				}
				writeResponse(e, uri);
			}
		} else {// Ϊ�ֿ����ʱ
			HttpChunk chunk = (HttpChunk) e.getMessage();
			if (chunk.isLast()) {
				readingChunks = false;
				// END OF CONTENT\r\n"
				HttpChunkTrailer trailer = (HttpChunkTrailer) chunk;
				if (!trailer.getHeaderNames().isEmpty()) {
					for (String name : trailer.getHeaderNames()) {
						for (String value : trailer.getHeaders(name)) {
							System.out.println("TRAILING HEADER: " + name + " = " + value + "\r\n");
						}
					}
				}
				writeResponse(e, "/");
			} else {
				System.out.println("CHUNK: " + chunk.getContent().toString(CharsetUtil.UTF_8)
						+ "\r\n");
			}
		}
	}

	private void writeResponse(MessageEvent e, String uri) {
		// ����Connection�ײ����ж��Ƿ�Ϊ�־�����
		boolean keepAlive = isKeepAlive(request);

		// Build the response object.
		HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
		response.setStatus(HttpResponseStatus.OK);
		// ����˿���ͨ��location�ײ����ͻ��˵���ĳ����Դ�ĵ�ַ��
		// response.addHeader("Location", uri);
		if (keepAlive) {
			// Add 'Content-Length' header only for a keep-alive connection.
			response.setHeader(CONTENT_LENGTH, response.getContent().readableBytes());
		}
		// �õ��ͻ��˵�cookie��Ϣ�����ٴ�д���ͻ���
		String cookieString = request.getHeader(COOKIE);
		if (cookieString != null) {
			CookieDecoder cookieDecoder = new CookieDecoder();
			Set<Cookie> cookies = cookieDecoder.decode(cookieString);
			if (!cookies.isEmpty()) {
				CookieEncoder cookieEncoder = new CookieEncoder(true);
				for (Cookie cookie : cookies) {
					cookieEncoder.addCookie(cookie);
				}
				response.addHeader(SET_COOKIE, cookieEncoder.encode());
			}
		}
		final String path = Config.getRealPath(uri);
		File localFile = new File(path);
		// ����ļ����ػ��߲�����
		if (localFile.isHidden() || !localFile.exists()) {
			// �߼�����
			return;
		}
		// �������·��ΪĿ¼
		if (localFile.isDirectory()) {
			// �߼�����
			return;
		}
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(localFile, "r");
			long fileLength = raf.length();
			response.setHeader(HttpHeaders.Names.CONTENT_LENGTH, String.valueOf(fileLength));
			Channel ch = e.getChannel();
			ch.write(response);
			// ������Ҫ������ϰ��http�ķ�����head������get�������ƣ����Ƿ���������Ӧ��ֻ�����ײ������᷵��ʵ������岿��
			if (!request.getMethod().equals(HttpMethod.HEAD)) {
				ch.write(new ChunkedFile(raf, 0, fileLength, 8192));//8kb
			}
		} catch (Exception e2) {
			e2.printStackTrace();
		} finally {
			if (keepAlive) {
				response.setHeader(CONTENT_LENGTH, response.getContent().readableBytes());
			}
			if (!keepAlive) {
				e.getFuture().addListener(ChannelFutureListener.CLOSE);
			}
		}
	}

	private void send100Continue(MessageEvent e) {
		HttpResponse response = new DefaultHttpResponse(HTTP_1_1, CONTINUE);
		e.getChannel().write(response);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		e.getCause().printStackTrace();
		e.getChannel().close();
	}

}
