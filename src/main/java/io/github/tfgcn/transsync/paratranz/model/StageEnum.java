package io.github.tfgcn.transsync.paratranz.model;

import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * desc: 词条状态
 *
 * @author yanmaoyuan
 */
@Getter
public enum StageEnum {
    UNTRANSLATED(0, "未翻译"),
    TRANSLATED(1, "已翻译"),
    DOUBTED(2, "有疑问"),
    CHECKED(3, "已检查"),
    REVIEWED(5, "已审核"),// 未开启二次校对的项目审核词条时会直接设为此状态
    LOCKED(9, "已锁定"),// 此状态下仅管理员可解锁，词条强制按译文导出
    HIDDEN(-1, "已隐藏")// 此状态下词条强制按原文导出
    ;
    private final Integer value;
    private final String desc;

    StageEnum(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static final Map<Integer, StageEnum> CACHE;
    static {
        Map<Integer, StageEnum> map = new HashMap<>();
        for (StageEnum stage : StageEnum.values()) {
            map.put(stage.value, stage);
        }
        CACHE = Collections.unmodifiableMap(map);// immutable
    }

    public static StageEnum of(Integer value) {
        return CACHE.get(value);
    }
}