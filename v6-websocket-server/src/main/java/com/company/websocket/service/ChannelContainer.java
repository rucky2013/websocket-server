package com.company.websocket.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA.
 * User: BG244210
 * Date: 26/10/2017
 * Time: 17:38
 * To change this template use File | Settings | File Templates.
 * Description:
 */

@Component
public class ChannelContainer {
    private static final Logger LOG = LoggerFactory.getLogger(ChannelHandler.class);
    public  static Map<String, ChannelHandler> GLOBAL_CHANNEL_MAP = new ConcurrentHashMap<String, ChannelHandler>(); // <sessionId, HandlerService>



}
