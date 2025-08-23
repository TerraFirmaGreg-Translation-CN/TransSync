package io.github.tfgcn.transsync.paratranz.model.files;

import lombok.Data;

import java.util.Date;
import java.util.Map;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
@Data
public class FilesDto {
    private Integer id;
    private Date createdAt;
    private Date updatedAt;
    private Date modifiedAt;
    private String name;
    private Integer project;
    private String format;
    private Integer total;
    private Integer translated;
    private Integer disputed;
    private Integer checked;
    private Integer reviewed;
    private Integer hidden;
    private Integer locked;
    private Integer words;
    private String hash;
    private String folder;
    private FileProgressDto progress;
    private Map<String, Object> extra;

}
