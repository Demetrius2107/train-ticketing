package com.trainticketing.business.config;

import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;

/**
 * <p>Title: BusinessApplication</p>
 * <p>Description: 业务服务启动类：承载车站/车次/车厢/座位/每日排班/余票/订单等核心业务域（阶段1）</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
@SpringBootApplication
@ComponentScan("com.trainticketing")
@MapperScan("com.trainticketing.*.mapper")
public class BusinessApplication {

  private static final Logger LOG = LoggerFactory.getLogger(BusinessApplication.class);

  public static void main(String[] args) {
    SpringApplication app = new SpringApplication(BusinessApplication.class);
    Environment env = app.run(args).getEnvironment();
    LOG.info("启动成功！！");
    LOG.info("测试地址: \thttp://127.0.0.1:{}{}/hello", env.getProperty("server.port"), env.getProperty("server.servlet.context-path"));
  }

}
