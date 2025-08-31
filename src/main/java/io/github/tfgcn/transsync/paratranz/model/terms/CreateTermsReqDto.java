package io.github.tfgcn.transsync.paratranz.model.terms;

import lombok.Data;

import java.util.List;

/**
 * desc: 查询术语请求参数
 *
 * @author yanmaoyuan
 */
@Data
public class CreateTermsReqDto {
    private String pos;// 术语词性 noun, verb, adj, adv
    private String term;// 术语原文 apple
    private String translation;// 术语译文 苹果
    private String note;// 术语注释 这是一条关于苹果的术语
    private List<String> variants;// 术语原文的其他形式（复数形式、其他时态等）
    private Boolean caseSensitive;// 术语匹配时是否大小写敏感
}