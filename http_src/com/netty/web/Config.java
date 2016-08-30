package com.netty.web;

public class Config {
	public static String getRealPath(String uri) {
		StringBuilder sb=new StringBuilder("/nettyDemo/web");
		sb.append(uri);
		if (!uri.endsWith("/")) {
			sb.append('/');
		}
		return sb.toString();
	}
}
