package com.company.websocket.util;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * Created with IntelliJ IDEA.
 * User: BG244210
 * Date: 26/10/2017
 * Time: 14:30
 * To change this template use File | Settings | File Templates.
 * Description:
 */
public class ResourceHelper {
    private static final String COFIG_FILE = "environment.properties";

    private static Configuration config;

    private static ResourceHelper instance = new ResourceHelper();

    private ResourceHelper() {
        try {
            config = new PropertiesConfiguration(COFIG_FILE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ResourceHelper getInstance() {
        return instance;
    }

    public String getStrValue(String key) {
        return config.getString(key);
    }

    public int getIntValue(String key) {
        return config.getInt(key);
    }
}
