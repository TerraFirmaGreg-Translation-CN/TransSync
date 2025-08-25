package io.github.tfgcn.transsync.paratranz.model.projects;

import lombok.Data;

import java.util.Date;

/**
 * desc: 项目统计信息
 *
 * @author yanmaoyuan
 */
@Data
public class ProjectStatsDto {
    private Integer id;
    private Date deletedAt;
    private Integer rank;
    private Date modifiedAt;
    private int total;
    private int translated;
    private int disputed;
    private int checked;
    private int reviewed;
    private int hidden;
    private int locked;
    private int words;
    private int members;
    private double tp;// translate 进度
    private double cp;// check 进度
    private double rp;// review 进度
}
