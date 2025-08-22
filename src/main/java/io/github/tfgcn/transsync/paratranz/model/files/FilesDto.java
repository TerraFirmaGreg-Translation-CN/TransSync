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
    /**
     * {
     *     "id": 421,
     *     "createdAt": "2021-01-11T03:19:52.818Z",
     *     "updatedAt": "2021-01-11T03:19:52.818Z",
     *     "modifiedAt": "2021-01-11T03:19:52.818Z",
     *     "name": "path/to/filename.csv",
     *     "project": 1,
     *     "format": "ssv",
     *     "total": 1453,
     *     "translated": 1452,
     *     "disputed": 2,
     *     "checked": 841,
     *     "reviewed": 272,
     *     "hidden": 2,
     *     "locked": 1,
     *     "words": 6421,
     *     "hash": "928ca86273fd6ea36f0eebd4405eb85e"
     *   }
     */
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
