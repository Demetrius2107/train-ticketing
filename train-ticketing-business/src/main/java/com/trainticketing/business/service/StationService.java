package com.trainticketing.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.trainticketing.business.domain.Station;
import com.trainticketing.business.mapper.StationMapper;
import com.trainticketing.business.req.StationSaveReq;
import com.trainticketing.business.resp.StationQueryResp;
import com.trainticketing.common.exception.BusinessException;
import com.trainticketing.common.exception.BusinessExceptionEnum;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * <p>Title: StationService</p>
 * <p>Description: 车站管理服务：新增（名称唯一性校验）、关键字查询、删除</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
@Service
public class StationService {

    private static final Logger LOG = LoggerFactory.getLogger(StationService.class);

    @Resource
    private StationMapper stationMapper;

    /**
     * 新增车站。车站名称全局唯一，重名时抛业务异常；
     * 代码层校验 + 数据库 uk_name 唯一索引兜底（双保险防重复）。
     *
     * @param req 车站新增请求
     * @return 新增车站ID
     */
    public Long save(StationSaveReq req) {
        Station stationDB = stationMapper.selectByName(req.getName());
        if (ObjectUtil.isNotNull(stationDB)) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_STATION_NAME_UNIQUE_ERROR);
        }
        Station station = new Station();
        station.setId(IdUtil.getSnowflakeNextId());
        station.setName(req.getName());
        station.setNamePinyin(req.getNamePinyin());
        station.setNamePy(req.getNamePy());
        station.setCity(req.getCity());
        station.setCreateTime(new Date());
        station.setUpdateTime(new Date());
        stationMapper.insert(station);
        return station.getId();
    }

    /**
     * 按关键字查询车站列表（名称/拼音全拼/拼音简拼/城市模糊匹配）
     *
     * @param keyword 查询关键字，可空（空则返回全部）
     * @return 车站列表
     */
    public List<StationQueryResp> queryList(String keyword) {
        List<Station> stationList = stationMapper.selectList(keyword);
        List<StationQueryResp> respList = new ArrayList<>();
        if (CollUtil.isNotEmpty(stationList)) {
            for (Station station : stationList) {
                respList.add(BeanUtil.copyProperties(station, StationQueryResp.class));
            }
        }
        return respList;
    }

    /**
     * 按主键删除车站
     * 注意：当前为物理删除，若后续车次/经停站引用该车站，需补充引用保护或改为软删除。
     *
     * @param id 车站ID
     */
    public void delete(Long id) {
        stationMapper.deleteById(id);
    }
}
