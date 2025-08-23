package io.github.tfgcn.transsync.paratranz.model.strings;

import lombok.Data;
import java.util.Date;

@Data
public class User {
    private Integer id;
    private String username;
    private String nickname;
    private String avatar;
    private Date lastVisit;
}