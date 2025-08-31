package io.github.tfgcn.transsync.paratranz.model.terms;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * desc: 查询术语请求参数
 *
 * @author yanmaoyuan
 */
@Data
public class GetTermsReqDto {
    /**
     * 页码
     * Default value : 1
     */
    private Integer page;
    /**
     * 每页数量
     * Default value : 50
     */
    private Integer pageSize;

    public Map<String, Object> asMap() {
        Map<String, Object> map = new HashMap<>();
        if (page != null) {
            map.put("page", page);
        }
        if (pageSize != null) {
            map.put("pageSize", pageSize);
        }
        return map;
    }
}