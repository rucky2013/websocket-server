package com.company.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: BG244210
 * Date: 26/10/2017
 * Time: 11:53
 * To change this template use File | Settings | File Templates.
 * Description:
 */
public class Bootstrap {
    private static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);
    private volatile static boolean isTerminal = false;

    public static void main(String[] args) {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(
                "classpath*:spring-db.xml",
                "classpath*:spring-dao.xml",
                "classpath*:spring-service.xml");

        if (logger.isInfoEnabled()) {
            logger.info("V6-WEBSOCKET 启动成功");
        }
        try {
            while (!isTerminal) {
                TimeUnit.MINUTES.sleep(10);
            }
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
            Thread.currentThread().interrupt();
            isTerminal = true;
        } finally {
            applicationContext.destroy();
        }
    }
}
