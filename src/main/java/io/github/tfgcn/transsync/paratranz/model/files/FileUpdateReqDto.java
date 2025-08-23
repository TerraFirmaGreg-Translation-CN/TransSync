package io.github.tfgcn.transsync.paratranz.model.files;

import lombok.Data;

import java.util.Map;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
@Data
public class FileUpdateReqDto {
    private String name;
    private Map<String, Object> extra;
}
