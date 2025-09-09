package io.github.tfgcn.transsync.paratranz.model.strings;

import lombok.Data;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
@Data
public class CreateStringsReqDto {
    private String key;
    private String original;
    private String translation;
    private FileInfo file;
    private Integer stage;
    private String context;
}
