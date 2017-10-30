package com.company.websocket.service;

import com.alibaba.fastjson.JSON;
import com.company.biz.constant.BizEnumConstants;
import com.company.websocket.context.BizChannelHandlerContext;
import com.company.websocket.util.ClientRequest;
import com.google.common.base.Strings;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;


/***
 *
 */
public class ChannelHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ChannelHandler.class);

    @Autowired
    private ChannelContainer channelContainer;

    private BizChannelHandlerContext ctx;

    public ChannelHandler(BizChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    /**
     *
     * @param requestId
     * @param channelHandler
     * @return
     */
    public static boolean register(String requestId, ChannelHandler channelHandler) {
        if (Strings.isNullOrEmpty(requestId) || ChannelContainer.GLOBAL_CHANNEL_MAP.containsKey(requestId)) {
            return false;
        }
        ChannelContainer.GLOBAL_CHANNEL_MAP.put(requestId, channelHandler);
        return true;
    }

    public static boolean logout(String sessionId) {
        if (Strings.isNullOrEmpty(sessionId) || !ChannelContainer.GLOBAL_CHANNEL_MAP.containsKey(sessionId)) {
            return false;
        }
        ChannelContainer.GLOBAL_CHANNEL_MAP.remove(sessionId);
        return true;
    }

    public void send(ClientRequest request) throws Exception {
        if (this.ctx == null || this.ctx.getCtx().isRemoved()) {
            throw new Exception("尚未握手成功，无法向客户端发送WebSocket消息");
        }
        this.ctx.getCtx().channel().write(new TextWebSocketFrame(JSON.toJSONString( request )));
        this.ctx.getCtx().flush();
    }




    /**
     * 通知所有机器有机器下线
     *
     * @param requestId
     */
    public static void notifyDownline(String requestId) {
        ChannelContainer.GLOBAL_CHANNEL_MAP.forEach((reqId, channelHandler) -> { // 通知有人下线
            ClientRequest serviceRequest = new ClientRequest();
            serviceRequest.setServiceId(BizEnumConstants.ServiceIdEnum.downline.code);
            serviceRequest.setSessionId(requestId);
            try {
                channelHandler.send(serviceRequest);
            } catch (Exception e) {
                LOG.warn("回调发送消息给客户端异常", e);
            }
        });
    }

    public String getUserName() {
        return ctx.getUserName();
    }

}
