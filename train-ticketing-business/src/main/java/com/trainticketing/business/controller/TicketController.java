package com.trainticketing.business.controller;

import com.trainticketing.business.resp.SeatRemainingResp;
import com.trainticketing.business.resp.TrainTicketResp;
import com.trainticketing.business.service.TicketService;
import com.trainticketing.common.resp.CommonResp;
import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Title: TicketController</p>
 * <p>Description: 余票查询接口（用户侧，走网关 http://localhost:8000/business/ticket/**）</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
@RestController
@RequestMapping("/ticket")
public class TicketController {

    @Resource
    private TicketService ticketService;

    /**
     * 按出发站/到达站/日期查询车次及区间余票
     *
     * @param fromStationId 出发站id
     * @param toStationId   到达站id
     * @param runDate       运行日期，如 2026-08-20
     * @return 车次余票列表
     */
    @GetMapping("/query")
    public CommonResp<List<TrainTicketResp>> query(@RequestParam Long fromStationId,
                                                   @RequestParam Long toStationId,
                                                   @RequestParam LocalDate runDate) {
        return new CommonResp<>(ticketService.queryByStations(fromStationId, toStationId, runDate));
    }

    /**
     * 查询指定排班某区间的余票
     *
     * @param dailyTrainId    排班ID
     * @param departStationId 出发站id
     * @param arriveStationId 到达站id
     * @return 各座位类型余票
     */
    @GetMapping("/query-remaining")
    public CommonResp<List<SeatRemainingResp>> queryRemaining(@RequestParam Long dailyTrainId,
                                                              @RequestParam Long departStationId,
                                                              @RequestParam Long arriveStationId) {
        return new CommonResp<>(ticketService.queryRemaining(dailyTrainId, departStationId, arriveStationId));
    }
}
