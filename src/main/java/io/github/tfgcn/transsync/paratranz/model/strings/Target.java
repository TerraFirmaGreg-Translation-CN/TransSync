package io.github.tfgcn.transsync.paratranz.model.strings;

import lombok.Data;

@Data
public class Target {
    private String key;
    private Integer stage;
    private Integer words;
    private String original;
    private String translation;
}