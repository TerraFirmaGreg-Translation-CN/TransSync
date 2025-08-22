package io.github.tfgcn.transsync.paratranz.model.users;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date updatedAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
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
