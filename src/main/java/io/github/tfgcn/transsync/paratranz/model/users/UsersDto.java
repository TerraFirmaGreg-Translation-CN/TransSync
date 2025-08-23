package io.github.tfgcn.transsync.paratranz.model.users;

import lombok.Data;

import java.util.Date;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
@Data
public class UsersDto {
    private Integer id;
    private Date createdAt;
    private Date updatedAt;
    private Date lastVisit;
    private String username;
    private String nickname;
    private String bio;
    private String avatar;
    private String email;
    private Integer github;
    private Integer role;
    private Integer credit;
    private Integer translated;
    private Integer edited;
    private Integer reviewed;
    private Integer commented;
    private Double points;
    private Date deletedAt;
    private Integer abusesCount;
    private Boolean isOnline;
}
