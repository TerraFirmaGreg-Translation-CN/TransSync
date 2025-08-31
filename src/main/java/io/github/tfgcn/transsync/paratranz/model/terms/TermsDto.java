package io.github.tfgcn.transsync.paratranz.model.terms;

import io.github.tfgcn.transsync.paratranz.model.users.UsersDto;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * desc: 术语
 *
 * @author yanmaoyuan
 */
@Data
public class TermsDto {
    private Integer id;
    private Date createdAt;
    private Date updatedAt;
    private UsersDto updatedBy;
    private String pos;// 术语词性 noun, verb, adj, adv
    private Integer uid;
    private String term;// 术语原文 apple
    private String translation;// 术语译文 苹果
    private String note;// 术语注释 这是一条关于苹果的术语
    private Integer project;// 项目ID
    private List<String> variants;// 术语原文的其他形式（复数形式、其他时态等）
    private Boolean caseSensitive;// 术语匹配时是否大小写敏感
    private Integer commentsCount;
    private UsersDto user;
}
