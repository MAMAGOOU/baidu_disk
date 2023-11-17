package com.rocket.pan.server.modules.file.mapper;

import com.rocket.pan.server.modules.file.context.FileSearchContext;
import com.rocket.pan.server.modules.file.context.QueryFileListContext;
import com.rocket.pan.server.modules.file.entity.RPanUserFile;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rocket.pan.server.modules.file.vo.FileSearchResultVO;
import com.rocket.pan.server.modules.file.vo.RPanUserFileVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author 19750
 * @description 针对表【r_pan_user_file(用户文件信息表)】的数据库操作Mapper
 * @createDate 2023-11-11 14:40:41
 * @Entity com.rocket.pan.server.modules.file.entity.RPanUserFile
 */
public interface RPanUserFileMapper extends BaseMapper<RPanUserFile> {

    /**
     * 查询用户的文件列表
     *
     * @param context
     * @return
     */
    List<RPanUserFileVO> selectFileList(@Param(value = "param") QueryFileListContext context);

    /**
     * 文件搜索
     *
     * @param context
     * @return
     */
    List<FileSearchResultVO> searchFile(@Param(value = "param") FileSearchContext context);
}




