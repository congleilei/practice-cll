package com.cll.cllcat;


import com.cll.servlet.CllResponse;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.internal.StringUtil;

public class HttpCllResponse implements CllResponse {

  private HttpRequest request;
  private ChannelHandlerContext context;
  private String contentType;

  public HttpCllResponse(HttpRequest request, ChannelHandlerContext context,String contentType) {
    this.request = request;
    this.context = context;
    this.contentType = contentType;
  }

  @Override
  public void write(String content) throws Exception {
    if (StringUtil.isNullOrEmpty(content)) {
      return;
    }

    //创建响应对象
    FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
        HttpResponseStatus.OK, Unpooled.wrappedBuffer(content.getBytes("UTF-8")));

    HttpHeaders headers = response.headers();
    //设置响应体类型
    headers.set(HttpHeaderNames.CONTENT_TYPE,contentType);
    //设置响应体长度
    headers.set(HttpHeaderNames.CONTENT_LENGTH,response.content().readableBytes());
    //设置缓存过期时间
    headers.set(HttpHeaderNames.EXPIRES,0);
    //如果请求是长连接，相应也应该是长连接
    if(HttpUtil.isKeepAlive(request)){
      headers.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
    }
    //把响应写入通道
    context.writeAndFlush(response);

  }

}
