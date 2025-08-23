package io.github.tfgcn.transsync.paratranz.model.strings;

import lombok.Data;
import java.util.Date;

@Data
public class ImportHistoryItem {
    private Long id;
    private Date createdAt;
    private String field;
    private Integer uid;
    private Long tid;
    private Integer project;
    private String key;
    private String from;
    private String to;
    private String type;
    private String operation;
}