package io.github.tfgcn.transsync.paratranz.model.files;

import lombok.Data;

import java.io.File;
import java.util.Date;
import java.util.Map;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
@Data
public class UploadFileResp {
    private FilesDto file;
    private RevisionDto revision;
}
