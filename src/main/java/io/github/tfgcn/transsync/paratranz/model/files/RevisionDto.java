package io.github.tfgcn.transsync.paratranz.model.files;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
@Data
public class RevisionDto {
    private Integer file;
    private String name;
    private String filename;
    private Integer uid;
    private Integer project;
    private String type;// create
    private Integer insert;
    private String hash;
    private Integer id;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date createdAt;
    private Integer update;
    private Integer remove;
    private Boolean force;
    private Boolean incremental;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date revertedAt;
}
