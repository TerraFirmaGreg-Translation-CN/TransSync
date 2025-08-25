package io.github.tfgcn.transsync.service.model;

import lombok.Data;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
@Data
public class DownloadTranslationResult {
    private String fileName;
    private String status;// create, update, skip
    private int bytes;// 字节数
}