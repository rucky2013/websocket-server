package com.company.websocket.util;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Strings;
import com.google.gson.Gson;

public class ClientRequest {

    private static Gson gson = new Gson();

    private String sessionId;
    private int serviceId;
    private String userName;
    private String message;

    public String getSessionId() {
        return sessionId;
    }

    public ClientRequest setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public int getServiceId() {
        return serviceId;
    }

    public ClientRequest setServiceId(int serviceId) {
        this.serviceId = serviceId;
        return this;
    }

    public String getUserName() {
        return userName;
    }

    public ClientRequest setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public ClientRequest setMessage(String message) {
        this.message = message;
        return this;
    }



}
