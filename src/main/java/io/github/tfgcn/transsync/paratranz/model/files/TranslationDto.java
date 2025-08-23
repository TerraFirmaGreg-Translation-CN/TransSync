package io.github.tfgcn.transsync.paratranz.model.files;

import lombok.Data;

/**
 * desc:
 *
 * @author: yanmaoyuan
 */
@Data
public class TranslationDto {
    private Integer id;
    private String key;
    private String original;
    private String translation;
    private Integer stage;
}
