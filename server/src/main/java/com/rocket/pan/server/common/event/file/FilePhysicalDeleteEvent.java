package com.rocket.pan.server.common.event.file;

import com.rocket.pan.server.modules.file.entity.RPanUserFile;
import lombok.*;

import java.io.Serializable;
import java.util.List;

/**
 * 文件被物理删除的事件实体
 *
 * @author 19750
 * @version 1.0
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
public class FilePhysicalDeleteEvent implements Serializable {
    private static final long serialVersionUID = 3959988542308316628L;

    /**
     * 所有被物理删除的文件实体集合
     */
    private List<RPanUserFile> allRecords;

    public FilePhysicalDeleteEvent(List<RPanUserFile> allRecords) {
        this.allRecords = allRecords;
    }
}
