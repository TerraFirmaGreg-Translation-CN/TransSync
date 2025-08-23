package io.github.tfgcn.transsync.paratranz.model.files;

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
    private Date createdAt;
    private Integer update;
    private Integer remove;
    private Boolean force;
    private Boolean incremental;
    private Date revertedAt;
}
