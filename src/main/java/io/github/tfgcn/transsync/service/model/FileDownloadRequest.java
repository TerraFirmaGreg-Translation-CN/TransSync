package io.github.tfgcn.transsync.service.model;

import io.github.tfgcn.transsync.Constants;
import io.github.tfgcn.transsync.paratranz.model.files.FilesDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * desc: 文件下载请求
 *
 * @author yanmaoyuan
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class FileDownloadRequest extends FilesDto {
    private String sourceFilePath;// relative path of workspace
    private String translationFilePath;// relative path of workspace

    public String getSourceFileFolder() {
        if (sourceFilePath == null) {
            return "";
        }
        int index = sourceFilePath.lastIndexOf(Constants.SEPARATOR);
        return index > -1 ? sourceFilePath.substring(0, index) : "";
    }

    public String getTranslationFileFolder() {
        if (translationFilePath == null) {
            return "";
        }
        int index = translationFilePath.lastIndexOf(Constants.SEPARATOR);
        return index > -1 ? translationFilePath.substring(0, index) : "";
    }
}