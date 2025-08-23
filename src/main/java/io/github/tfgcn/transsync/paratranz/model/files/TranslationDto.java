package io.github.tfgcn.transsync.paratranz.model.files;

import lombok.Data;

/**
 * desc: 译文
 *
 * @author yanmaoyuan
 */
@Data
public class TranslationDto {
    private Integer id;
    private String key;// 词条键值，文件内必须唯一
    private String original;// 词条原文
    private String translation;// 词条译文
    private Integer stage;// 词条状态
    private String context;
}
