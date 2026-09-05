package com.trainticketing.business.config;

import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * <p>Title: BusinessApplication</p>
 * <p>Description: 业务服务启动类：承载车站/车次/车厢/座位/每日排班/余票/订单等核心业务域（阶段1）</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 * @since 1.0
 */
@SpringBootApplication
@ComponentScan("com.trainticketing")
@MapperScan("com.trainticketing.*.mapper")
@EnableScheduling
public class BusinessApplication {

    private static final Logger LOG = LoggerFactory.getLogger(BusinessApplication.class);

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(BusinessApplication.class);
        Environment env = app.run(args).getEnvironment();
        LOG.info("Business服务启动成功，profile: {}", String.join(",", env.getActiveProfiles()));
    }

}
