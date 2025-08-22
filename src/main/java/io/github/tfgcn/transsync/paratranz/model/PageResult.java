package io.github.tfgcn.transsync.paratranz.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
@Data
public class PageResult<T> {
    private int page;
    private int pageSize;
    private int rowCount;
    private int pageCount;
    private List<T> results;
}
