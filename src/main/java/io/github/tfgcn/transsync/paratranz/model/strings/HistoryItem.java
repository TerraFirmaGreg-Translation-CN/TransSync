package io.github.tfgcn.transsync.paratranz.model.strings;

import lombok.Data;
import java.util.Date;

@Data
public class HistoryItem {
    private Long id;
    private Date createdAt;
    private String field;
    private Integer uid;
    private Long tid;
    private Integer project;
    private String from;
    private String to;
    private Target target;
    private String operation;
    private User user;
}