package io.github.tfgcn.transsync.paratranz.model.strings;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * desc: 词条
 *
 * @author yanmaoyuan
 */
@Data
public class StringsDto {
    private Long id;
    private Date createdAt;
    private Date updatedAt;
    private String key;
    private String original;
    private String translation;
    private FileInfo file;
    private Integer stage;
    private Integer project;
    private Integer uid;
    private Object extra;
    private String context;
    private Integer words;
    private Integer version;
    private User user;
    private List<Comment> comments;
    private List<ImportHistoryItem> importHistory;
    private List<HistoryItem> history;
    private Integer fileId;
}