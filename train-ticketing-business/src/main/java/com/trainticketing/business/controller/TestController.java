package com.trainticketing.business.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Title: TestController</p>
 * <p>Description: 服务连通性测试接口</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
@RestController
public class TestController {

  /**
   * 服务健康检查
   *
   * @return 连通性提示
   */
  @GetMapping("/hello")
  public String hello() {
    return "Hello TrainTicketing business!";
  }
}
