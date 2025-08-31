package io.github.tfgcn.transsync.paratranz.model.strings;

import lombok.Data;

import java.util.List;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
@Data
public class UpdateStringsReqDto {
    private String op;// 操作类型，update - 更新，delete - 删除
    private List<Integer> id;// 需要操作的词条的id
    private Integer stage;// 词条状态 default 0,
    private String translation;// 词条翻译
}
