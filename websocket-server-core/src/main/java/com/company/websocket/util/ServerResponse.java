package com.company.websocket.util;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Strings;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public class ServerResponse {

    private String sessionId;
    private int serviceId;
    private boolean isSucc;
    private String userName;
    private String message;
    private Map<String, String> hadOnline = new HashMap<String, String>(); // <sessionId, name>

    public String getSessionId() {
        return sessionId;
    }

    public ServerResponse setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public int getServiceId() {
        return serviceId;
    }

    public ServerResponse setServiceId(int serviceId) {
        this.serviceId = serviceId;
        return this;
    }

    public boolean getIsSucc() {
        return isSucc;
    }

    public ServerResponse setIsSucc(boolean isSucc) {
        this.isSucc = isSucc;
        return this;
    }

    public String getUserName() {
        return userName;
    }

    public ServerResponse setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public ServerResponse setMessage(String message) {
        this.message = message;
        return this;
    }

    public Map<String, String> getHadOnline() {
        return hadOnline;
    }

    public ServerResponse setHadOnline(Map<String, String> hadOnline) {
        this.hadOnline = hadOnline;
        return this;
    }


}
