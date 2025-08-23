package io.github.tfgcn.transsync.paratranz.model.files;

import lombok.Data;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
@Data
public class FileUploadRespDto {
    private FilesDto file;
    private RevisionDto revision;
    private String status;// 如果重复上传相同文件，这个字段将会返回 "hasMatched"
}
