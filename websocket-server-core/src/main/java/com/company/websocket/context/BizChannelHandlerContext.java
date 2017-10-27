package com.company.websocket.context;

import io.netty.channel.ChannelHandlerContext;

/**
 * Created with IntelliJ IDEA.
 * User: BG244210
 * Date: 26/10/2017
 * Time: 17:52
 * To change this template use File | Settings | File Templates.
 * Description:
 */
public class BizChannelHandlerContext {

    private ChannelHandlerContext ctx;
    private String userName;

    public BizChannelHandlerContext(ChannelHandlerContext ctx, String userName) {
        this.ctx = ctx;
        this.userName = userName;
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public void setCtx(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
