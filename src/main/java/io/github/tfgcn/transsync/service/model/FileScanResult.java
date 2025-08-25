package io.github.tfgcn.transsync.service.model;

import lombok.Data;

/**
 * desc: 文件扫描结果
 *
 * @author yanmaoyuan
 */
@Data
public class FileScanResult {
    private String sourceFilePath;// relative path of workspace
    private String translationFilePath;// relative path of workspace
}