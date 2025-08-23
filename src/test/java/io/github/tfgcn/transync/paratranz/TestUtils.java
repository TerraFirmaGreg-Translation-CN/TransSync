package io.github.tfgcn.transync.paratranz;

import io.github.tfgcn.transsync.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * desc: 测试用工具集
 *
 * @author yanmaoyuan
 */
@Slf4j
public final class TestUtils {
    private TestUtils() {}

    // 项目ID，默认是TerraFirmaGreg汉化组项目，如果要测试其他项目，请修改这个值
    public static final Integer PROJECT_ID = Constants.DEFAULT_PROJECT_ID;// change this if you want to test with other projects

    // 测试用文件，存放在项目根目录下
    public static final String TEST_FOLDER = "test";
    public static final String TEST_FILE = "test/zh_cn.json";

    // 本地测试文件
    public static final String TEST_EN_FILE = "test/en_us.json";
    public static final String TEST_EN_CONTENT = "{\"key\":\"Hello\"}";
    public static final String TEST_ZH_FILE = "test/zh_cn.json";
    public static final String TEST_ZH_CONTENT = "{\"key\":\"你好\"}";
    public static final String TEST_FILE_ID_NAME = "test/file_id.txt";

    public static void saveTestFileId(Integer fileId) throws IOException {
        File file = new File(TEST_FILE_ID_NAME);
        if (file.exists()) {
            String oldFileId = FileUtils.readFileToString(file, "UTF-8");
            if (oldFileId.equals(fileId.toString())) {
                return;
            }
        }

        log.info("saveTestFileId: {}", fileId);
        file.getParentFile().mkdirs();
        FileUtils.writeStringToFile(file, fileId.toString(), "UTF-8");
    }

    public static Integer getTestFileId() throws IOException {
        File file = new File(TEST_FILE_ID_NAME);
        if (!file.exists()) {
            throw new IOException("File id not exists, please run testUploadFile() first.");
        }
        try {
            return Integer.parseInt(FileUtils.readFileToString(file, "UTF-8"));
        } catch (NumberFormatException e) {
            throw new IOException("Invalid file id: " + file.getAbsolutePath());
        }
    }

    public static void deleteTestFileId() throws IOException {
        File file = new File(TEST_FILE_ID_NAME);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 获取测试文件。
     * 若不存在则自动生成
     * @return
     */
    public static File getOrCreateTestFile(String filename, String content) throws IOException {
        File file = new File(filename);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            file.createNewFile();
            FileUtils.writeStringToFile(file, content, "UTF-8");
        }
        return file;
    }
}
