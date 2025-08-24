package io.github.tfgcn.transsync.paratranz.model.strings;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
@Data
public class GetStringsReqDto {
    private Integer page;// 页码 默认1
    private Integer pageSize;// 每页数量 默认50
    private Integer file;// 词条所在文件ID
    private Integer stage;// 筛选词条状态 默认0. Available values : 0, 1, 2, 3, 5, 9, -1

    public Map<String, Object> asMap() {
        Map<String, Object> map = new HashMap<>();
        if (page != null) {
            map.put("page", page);
        }
        if (pageSize != null) {
            map.put("pageSize", pageSize);
        }
        if (file != null) {
            map.put("file", file);
        }
        if (stage != null) {
            map.put("stage", stage);
        }
        return map;
    }
}