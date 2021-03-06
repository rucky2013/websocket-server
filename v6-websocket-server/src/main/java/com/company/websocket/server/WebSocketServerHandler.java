package com.company.websocket.server;

import com.alibaba.fastjson.JSON;
import com.company.biz.constant.BizEnumConstants;
import com.company.websocket.context.BizChannelHandlerContext;
import com.company.websocket.service.ChannelContainer;
import com.company.websocket.service.ChannelHandler;
import com.company.websocket.util.ClientRequest;
import com.company.websocket.util.ServerResponse;
import com.google.common.base.Strings;
import com.google.gson.JsonSyntaxException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * WebSocket服务端Handler
 */
public class WebSocketServerHandler extends SimpleChannelInboundHandler<Object> {
    private static final Logger LOG = LoggerFactory.getLogger(WebSocketServerHandler.class.getName());

    private WebSocketServerHandshaker handshaker;
    private ChannelHandlerContext ctx;
    private String sessionId;

    @Override
    public void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) { // 传统的HTTP接入
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) { // WebSocket接入
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOG.error("WebSocket异常", cause);
        ctx.close();
        LOG.info(sessionId + " 	注销");
        ChannelHandler.logout(sessionId); // 注销
        ChannelHandler.notifyDownline(sessionId); // 通知有人下线
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        LOG.info("WebSocket关闭");
        super.close(ctx, promise);
        LOG.info(sessionId + " 注销");
        ChannelHandler.logout(sessionId); // 注销
        ChannelHandler.notifyDownline(sessionId); // 通知有人下线
    }

    /**
     * 处理Http请求，完成WebSocket握手<br/>
     * 注意：WebSocket连接第一次请求使用的是Http
     *
     * @param ctx
     * @param request
     * @throws Exception
     */
    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        // 如果HTTP解码失败，返回HHTP异常
        if (!request.getDecoderResult().isSuccess() || (!"websocket".equals(request.headers().get("Upgrade")))) {
            sendHttpResponse(ctx, request, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }

        // 正常WebSocket的Http连接请求，构造握手响应返回
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory("ws://" + request.headers().get(HttpHeaders.Names.HOST), null, false);
        handshaker = wsFactory.newHandshaker(request);
        if (handshaker == null) { // 无法处理的websocket版本
            WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse(ctx.channel());
        } else { // 向客户端发送websocket握手,完成握手
            handshaker.handshake(ctx.channel(), request);
            // 记录管道处理上下文，便于服务器推送数据到客户端
            this.ctx = ctx;
        }
    }

    /**
     * 处理Socket请求
     *
     * @param ctx
     * @param frame
     * @throws Exception
     */
    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        // 判断是否是关闭链路的指令
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }
        // 判断是否是Ping消息
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        // 当前只支持文本消息，不支持二进制消息
        if (!(frame instanceof TextWebSocketFrame)) {
            throw new UnsupportedOperationException("当前只支持文本消息，不支持二进制消息");
        }

        // 处理来自客户端的WebSocket请求
        try {
            ClientRequest request = JSON.parseObject(((TextWebSocketFrame) frame).text() ,ClientRequest.class);
            ServerResponse response = new ServerResponse();
            response.setServiceId(request.getServiceId());
            if (BizEnumConstants.ServiceIdEnum.online.code.intValue() == request.getServiceId()) { // 客户端注册
                String requestId = request.getSessionId();
                if (Strings.isNullOrEmpty(requestId)) {
                    response.setIsSucc(false).setMessage("requestId不能为空");
                    return;
                } else if (Strings.isNullOrEmpty(request.getUserName())) {
                    response.setIsSucc(false).setMessage("name不能为空");
                    return;
                } else if (ChannelContainer.GLOBAL_CHANNEL_MAP.containsKey(requestId)) {
                    response.setIsSucc(false).setMessage("您已经注册了，不能重复注册");
                    return;
                }
                if (!ChannelHandler.register(requestId,
                        new ChannelHandler( new BizChannelHandlerContext(  ctx, request.getUserName())))) {
                    response.setIsSucc(false).setMessage("注册失败");
                } else {
                    response.setIsSucc(true).setMessage("注册成功");

                    ChannelContainer.GLOBAL_CHANNEL_MAP.forEach((reqId, callBack) -> {
                        response.getHadOnline().put(reqId, ((ChannelHandler) callBack).getUserName()); // 将已经上线的人员返回

                        if (!reqId.equals(requestId)) {
                            ClientRequest serviceRequest = new ClientRequest();
                            serviceRequest.setServiceId(BizEnumConstants.ServiceIdEnum.online.code);
                            serviceRequest.setSessionId(requestId);
                            serviceRequest.setUserName(request.getUserName());
                            try {
                                callBack.send(serviceRequest); // 通知有人上线
                            } catch (Exception e) {
                                LOG.warn("回调发送消息给客户端异常", e);
                            }
                        }
                    });
                }
                sendWebSocket(JSON.toJSONString( response ));
                this.sessionId = requestId; // 记录会话id，当页面刷新或浏览器关闭时，注销掉此链路
            } else if (BizEnumConstants.ServiceIdEnum.send_message.code.intValue() == request.getServiceId()) { // 客户端发送消息到聊天群
                String requestId = request.getSessionId();
                if (Strings.isNullOrEmpty(requestId)) {
                    response.setIsSucc(false).setMessage("requestId不能为空");
                } else if (Strings.isNullOrEmpty(request.getUserName())) {
                    response.setIsSucc(false).setMessage("name不能为空");
                } else if (Strings.isNullOrEmpty(request.getMessage())) {
                    response.setIsSucc(false).setMessage("message不能为空");
                } else {
                    response.setIsSucc(true).setMessage("发送消息成功");

                    ChannelContainer.GLOBAL_CHANNEL_MAP.forEach((reqId, callBack) -> { // 将消息发送到所有机器
                        ClientRequest serviceRequest = new ClientRequest();
                        serviceRequest.setServiceId(BizEnumConstants.ServiceIdEnum.receive_message.code);
                        serviceRequest.setSessionId(requestId);
                        serviceRequest.setUserName(request.getUserName());
                        serviceRequest.setMessage(request.getMessage());
                        try {
                            callBack.send(serviceRequest);
                        } catch (Exception e) {
                            LOG.warn("回调发送消息给客户端异常", e);
                        }
                    });
                }
                sendWebSocket(JSON.toJSONString( response ));
            } else if (BizEnumConstants.ServiceIdEnum.downline.code.intValue() == request.getServiceId()) { // 客户端下线
                String requestId = request.getSessionId();
                if (Strings.isNullOrEmpty(requestId)) {
                    sendWebSocket( JSON.toJSONString(  response.setIsSucc(false).setMessage("requestId不能为空") ));
                } else {
                    ChannelHandler.logout(requestId);
                    response.setIsSucc(true).setMessage("下线成功");

                    ChannelHandler.notifyDownline(requestId); // 通知有人下线

                    sendWebSocket(JSON.toJSONString( response ));
                }

            } else {
                sendWebSocket(JSON.toJSONString(  response.setIsSucc(false).setMessage("未知请求") ));
            }
        } catch (JsonSyntaxException e1) {
            LOG.warn("Json解析异常", e1);
        } catch (Exception e2) {
            LOG.error("处理Socket请求异常", e2);
        }
    }

    /**
     * Http返回
     *
     * @param ctx
     * @param request
     * @param response
     */
    private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest request, FullHttpResponse response) {
        // 返回应答给客户端
        if (response.getStatus().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(response.getStatus().toString(), CharsetUtil.UTF_8);
            response.content().writeBytes(buf);
            buf.release();
            HttpHeaders.setContentLength(response, response.content().readableBytes());
        }

        // 如果是非Keep-Alive，关闭连接
        ChannelFuture f = ctx.channel().writeAndFlush(response);
        if (!HttpHeaders.isKeepAlive(request) || response.getStatus().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    /**
     * WebSocket返回
     *
     * @param msg
     */
    public void sendWebSocket(String msg) throws Exception {
        if (this.handshaker == null || this.ctx == null || this.ctx.isRemoved()) {
            throw new Exception("尚未握手成功，无法向客户端发送WebSocket消息");
        }
        this.ctx.channel().write(new TextWebSocketFrame(msg));
        this.ctx.flush();
    }

}
