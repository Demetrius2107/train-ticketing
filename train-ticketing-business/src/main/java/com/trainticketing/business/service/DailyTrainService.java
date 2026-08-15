package com.trainticketing.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.trainticketing.business.domain.DailyTrain;
import com.trainticketing.business.domain.Train;
import com.trainticketing.business.enums.DailyTrainStatusEnum;
import com.trainticketing.business.mapper.DailyTrainMapper;
import com.trainticketing.business.mapper.TrainMapper;
import com.trainticketing.business.req.DailyTrainSaveReq;
import com.trainticketing.business.resp.DailyTrainQueryResp;
import com.trainticketing.common.exception.BusinessException;
import com.trainticketing.common.exception.BusinessExceptionEnum;
import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * <p>Title: DailyTrainService</p>
 * <p>Description: 每日排班服务：排班（唯一校验/继承车次模板）、查询、状态管理（停运/恢复）、删除</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
@Service
public class DailyTrainService {

    private static final Logger LOG = LoggerFactory.getLogger(DailyTrainService.class);

    @Resource
    private DailyTrainMapper dailyTrainMapper;

    @Resource
    private TrainMapper trainMapper;

    /**
     * 新增排班。业务规则：
     * 1. 车次必须已存在（引用校验）；
     * 2. 同一车次同一日期只能排一次（代码校验 + uk_train_date 唯一索引兜底）；
     * 3. 起终站与时刻继承车次模板，避免排班与模板不一致；
     * 4. 状态默认运行（DailyTrainStatusEnum.RUN），运行日期由 Req 校验不能早于今天。
     *
     * @param req 排班新增请求
     * @return 新增排班ID
     */
    public Long save(DailyTrainSaveReq req) {
        Train train = trainMapper.selectById(req.getTrainId());
        if (ObjectUtil.isNull(train)) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_TRAIN_NOT_EXIST);
        }
        DailyTrain dailyDB = dailyTrainMapper.selectByTrainAndDate(req.getTrainId(), req.getRunDate());
        if (ObjectUtil.isNotNull(dailyDB)) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_DAILY_TRAIN_EXIST);
        }
        DailyTrain dailyTrain = new DailyTrain();
        dailyTrain.setId(IdUtil.getSnowflakeNextId());
        dailyTrain.setTrainId(req.getTrainId());
        dailyTrain.setRunDate(req.getRunDate());
        dailyTrain.setStartStationId(train.getStartStationId());
        dailyTrain.setEndStationId(train.getEndStationId());
        dailyTrain.setStartTime(train.getStartTime());
        dailyTrain.setEndTime(train.getEndTime());
        dailyTrain.setStatus(DailyTrainStatusEnum.RUN.getCode());
        dailyTrain.setCreateTime(new Date());
        dailyTrain.setUpdateTime(new Date());
        dailyTrainMapper.insert(dailyTrain);
        return dailyTrain.getId();
    }

    /**
     * 按车次/日期查询排班列表
     *
     * @param trainId 车次ID，可空
     * @param runDate 运行日期，可空
     * @return 排班列表
     */
    public List<DailyTrainQueryResp> queryList(Long trainId, LocalDate runDate) {
        List<DailyTrain> dailyList = dailyTrainMapper.selectList(trainId, runDate);
        List<DailyTrainQueryResp> respList = new ArrayList<>();
        if (CollUtil.isNotEmpty(dailyList)) {
            for (DailyTrain dailyTrain : dailyList) {
                respList.add(BeanUtil.copyProperties(dailyTrain, DailyTrainQueryResp.class));
            }
        }
        return respList;
    }

    /**
     * 更新排班状态（0 停运 / 1 运行）。业务规则：状态必须合法（枚举校验），
     * 停运用于模拟列车停运公告场景；已存在订单/余票的排班停运属后续阶段的对账问题。
     *
     * @param id     排班ID
     * @param status 目标状态
     */
    public void updateStatus(Long id, String status) {
        if (ObjectUtil.isNull(DailyTrainStatusEnum.getByCode(status))) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_STATUS_INVALID);
        }
        dailyTrainMapper.updateStatus(id, status);
    }

    /**
     * 按主键删除排班
     * 注意：若已生成当日座位/订单，删除需补充引用保护（阶段1 暂以注释声明）。
     *
     * @param id 排班ID
     */
    public void delete(Long id) {
        dailyTrainMapper.deleteById(id);
    }
}
