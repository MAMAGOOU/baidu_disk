package com.rocket.pan.server.modules.share.mapper;

import com.rocket.pan.server.modules.share.entity.RPanShare;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rocket.pan.server.modules.vo.RPanShareUrlListVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author 19750
 * @description 针对表【r_pan_share(用户分享表)】的数据库操作Mapper
 * @createDate 2023-11-11 14:45:54
 * @Entity com.rocket.pan.server.modules.share.entity.RPanShare
 */
public interface RPanShareMapper extends BaseMapper<RPanShare> {
    /**
     * 查询用户的分享列表
     *
     * @param userId
     * @return
     */
    List<RPanShareUrlListVO> selectShareVOListByUserId(@Param("userId") Long userId);

    /**
     * 滚动查询已存在的分享ID集合
     * @param startId
     * @param limit
     * @return
     */
    List<Long> rollingQueryShareId(@Param("startId") long startId, @Param("limit") long limit);
}




