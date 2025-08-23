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
    private Integer credit;
    private Integer translated;
    private Integer edited;
    private Integer reviewed;
    private Integer commented;
    private Integer points;
}
