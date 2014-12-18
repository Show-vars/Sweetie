package com.showvars.sweetie.network;

import com.showvars.sweetie.SweetieApp;
import com.showvars.sweetie.foundation.Context;
import com.showvars.sweetie.foundation.Cookies;
import com.showvars.sweetie.foundation.Request;
import com.showvars.sweetie.foundation.RequestMethod;
import com.showvars.sweetie.foundation.Response;
import com.showvars.sweetie.foundation.Session;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.DefaultCookie;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.ServerCookieEncoder;
import io.netty.handler.stream.ChunkedStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger log = Logger.getLogger(HttpServer.class.getName());
    private final SweetieApp app;
    private HttpRequest httprequest;
    private Response resp;

    HttpServerHandler(SweetieApp app) {
        this.app = app;

    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) {
        this.httprequest = (HttpRequest) msg;

        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(httprequest.getUri());

        Request request = new Request(
                RequestMethod.valueOf(httprequest.getMethod().name()),
                httprequest.headers().get("HOST"),
                httprequest.getUri(), queryStringDecoder.path(),
                ctx.channel().remoteAddress());

        request.setParameters(queryStringDecoder.parameters());

        boolean needNewSession = true;

        Cookies cookiesDownload = new Cookies();
        Collection<Cookie> cookiesUpload = new ArrayList<>();

        String cookieString = httprequest.headers().get(HttpHeaders.Names.COOKIE);
        if (cookieString != null) {
            for (Cookie cookie : CookieDecoder.decode(cookieString)) {
                if (cookie.getName().equals("SWEETIESESSIONID")) {
                    Session session = app.getSession(UUID.fromString(cookie.getValue()));
                    if (session != null) {
                        session.update();
                        request.setSession(session);
                        needNewSession = false;
                    }
                    continue;
                }
                cookiesDownload.put(cookie.getName(), cookie);
            }

        }
        request.setCookies(cookiesDownload);

        if (needNewSession) {
            Session session = new Session();
            UUID sessionId = UUID.randomUUID();
            
            app.putSession(sessionId, session);
            request.setSession(session);
            
            cookiesUpload.add(new DefaultCookie("SWEETIESESSIONID", sessionId.toString()));
        }

        Context sctx = new Context(request, app);
        resp = app.getRouter().forward(sctx);

        HttpResponse response = new DefaultHttpResponse(
                HttpVersion.HTTP_1_1,
                ((LastHttpContent) msg).getDecoderResult().isSuccess()
                && resp != null
                        ? HttpResponseStatus.valueOf(resp.getStatus()) : HttpResponseStatus.BAD_REQUEST);

        response.headers().set(HttpHeaders.Names.CONTENT_TYPE, resp != null ? resp.getContentType() : "text/plain");

        response.headers().set(HttpHeaders.Names.SET_COOKIE, ServerCookieEncoder.encode(cookiesUpload));

        if (resp.getContentLength() >= 0) {
            response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, resp.getContentLength());
        }

        if (!HttpHeaders.isKeepAlive(httprequest)) {
            response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }

        response.headers().set(HttpHeaders.Names.SERVER, "Sweetie/0.0.1.Alpha"); // how it's beautiful!

        ctx.write(response);

        if (resp.getStream() != null) {
            ChannelFuture sendContentFuture = ctx.write(new HttpChunkedInput(
                    new ChunkedStream(resp.getStream(), 8192)),
                    ctx.newProgressivePromise());
        }
        ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

        if (!HttpHeaders.isKeepAlive(httprequest)) {
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.log(Level.SEVERE, "HttpServerHandler exception", cause);
        ctx.close();
    }

}