package io.github.tfgcn.transsync.paratranz.model.files;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date updatedAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
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
