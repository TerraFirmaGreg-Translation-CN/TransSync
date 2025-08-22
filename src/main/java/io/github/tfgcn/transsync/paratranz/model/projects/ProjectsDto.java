package io.github.tfgcn.transsync.paratranz.model.projects;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.tfgcn.transsync.paratranz.model.users.UsersDto;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
@Data
public class ProjectsDto {
    private int id;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date updatedAt;
    private int uid;
    private UsersDto user;
    private String name;
    private String logo;
    private String desc;
    private String source;
    private String dest;
    private int members;
    private String game;
    private String license;
    private int activeLevel;
    private int stage;
    private int privacy;
    private int download;
    private int issueMode;
    private int reviewMode;
    private int joinMode;

    private String abuse;
    private String rank;
    private Map<String, Object> extra;
    private Map<String, Object> stats;
    private List<String> relatedGames;
    private Boolean isPrivate;
    private String gameName;
    private Map<String, Object> formats;
}
