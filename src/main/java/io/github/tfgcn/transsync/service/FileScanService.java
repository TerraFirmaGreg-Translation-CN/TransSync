package io.github.tfgcn.transsync.service;

import io.github.tfgcn.transsync.service.model.FileScanRequest;
import io.github.tfgcn.transsync.service.model.FileScanResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * desc: 扫描文件服务
 *
 * @author yanmaoyuan
 */
@Slf4j
public class FileScanService {

    /**
     * 扫描原文，并映射生成译文路径
     *
     * @param request
     * @return
     * @throws IOException
     */
    public List<FileScanResult> scanAndMapFiles(FileScanRequest request) throws IOException {
        List<FileScanResult> results = new ArrayList<>();

        File workspace = new File(request.getWorkspace());
        if (!FileUtils.isDirectory(workspace)) {
            throw new IllegalArgumentException("工作空间必须是一个目录");
        }

        Path workspacePath = Paths.get(workspace.getCanonicalPath());
        log.warn("Set workspace to: {}", workspacePath);

        // 移除路径首个 "/" ，确保输入的是相对路径
        String sourceFilePattern;
        if (request.getSourceFilePattern().startsWith("/")) {
            sourceFilePattern = request.getSourceFilePattern().substring(1);
        } else {
            sourceFilePattern = request.getSourceFilePattern();
        }

        String translationFilePattern;
        if (request.getTranslationFilePattern().startsWith("/")) {
            translationFilePattern = request.getTranslationFilePattern().substring(1);
        } else {
            translationFilePattern = request.getTranslationFilePattern();
        }

        // 查找所有匹配的文件
        List<Path> matchedFiles = findFiles(workspacePath, sourceFilePattern, request.getIgnores());

        // 生成映射结果
        for (Path file : matchedFiles) {
            Path targetPath = generateTargetPath(
                    workspacePath,
                    file,
                    request.getSrcLang(),
                    request.getDestLang(),
                    translationFilePattern
            );

            // 转换为相对工作空间的路径，并统一使用"/"
            String sourceRelativePath = workspacePath.relativize(file).toString().replace("\\", "/");
            String targetRelativePath = workspacePath.relativize(targetPath).toString().replace("\\", "/");

            FileScanResult result = new FileScanResult();
            result.setSourceFilePath(sourceRelativePath);
            result.setTranslationFilePath(targetRelativePath);
            results.add(result);
        }

        // 按照源文件路径进行排序
        results.sort(Comparator.comparing(FileScanResult::getSourceFilePath));

        return results;
    }

    /**
     * 查找匹配模式的文件
     */
    private List<Path> findFiles(Path baseDir, String pattern, List<String> ignores) throws IOException {
        List<Path> result = new ArrayList<>();

        // 解析glob模式
        String globPattern = pattern;
        // 将rootDir声明为final
        final Path rootDir;

        // 提取glob模式中的根目录（第一个通配符之前的部分）
        int globStart = pattern.indexOf('*');
        int globQuestion = pattern.indexOf('?');
        int firstWildcard = Integer.MAX_VALUE;

        if (globStart != -1) firstWildcard = Math.min(firstWildcard, globStart);
        if (globQuestion != -1) firstWildcard = Math.min(firstWildcard, globQuestion);

        if (firstWildcard != Integer.MAX_VALUE) {
            String rootDirStr = pattern.substring(0, firstWildcard);
            rootDirStr = rootDirStr.substring(0, rootDirStr.lastIndexOf('/') + 1);
            rootDir = baseDir.resolve(rootDirStr).normalize();
            globPattern = pattern.substring(firstWildcard);
        } else {
            // 如果没有通配符，直接使用baseDir
            rootDir = baseDir;
        }

        // 使用Java NIO的Glob匹配器
        final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + globPattern);

        // 添加忽略能力
        final List<PathMatcher> ignoreMatchers;
        if (ignores == null || ignores.isEmpty()) {
            ignoreMatchers = Collections.emptyList();
        } else {
            ignoreMatchers = new ArrayList<>(ignores.size());
            for (String ignore : ignores) {
                // 支持glob模式的忽略规则，如**/.git/**、*.tmp等
                ignoreMatchers.add(FileSystems.getDefault().getPathMatcher("glob:" + ignore));
            }
        }

        // 遍历所有文件
        Files.walkFileTree(rootDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                Path relativeDir = rootDir.relativize(dir);
                // 检查目录是否需要被忽略
                for (PathMatcher matcher : ignoreMatchers) {
                    if (matcher.matches(relativeDir)) {
                        return FileVisitResult.SKIP_SUBTREE; // 跳过整个子目录
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                // 获取相对于根目录的路径用于匹配
                Path relativePath = rootDir.relativize(file);

                // 检查文件是否需要被忽略
                for (PathMatcher matcher : ignoreMatchers) {
                    if (matcher.matches(relativePath)) {
                        return FileVisitResult.CONTINUE; // 忽略该文件
                    }
                }

                // 检查文件是否匹配目标模式
                if (matcher.matches(relativePath)) {
                    result.add(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                log.warn("访问文件失败: {}", file, exc);
                return FileVisitResult.CONTINUE;
            }
        });

        return result;
    }

    /**
     * 生成目标文件路径
     * 关键改进：正确识别语言文件夹前后的路径部分
     */
    private Path generateTargetPath(
            Path workspacePath,
            Path sourceFile,
            String sourceLanguage,
            String targetLanguage,
            String translationPattern) {

        // 获取文件相对于工作空间的路径
        Path relativeToWorkspace = workspacePath.relativize(sourceFile);
        List<String> pathSegments = new ArrayList<>();
        relativeToWorkspace.forEach(segment -> pathSegments.add(segment.toString()));

        // 提取文件名（%original_file_name%）
        String originalFileName = pathSegments.get(pathSegments.size() - 1);

        // 找到源语言文件夹在路径中的位置
        int langIndex = -1;
        for (int i = 0; i < pathSegments.size(); i++) {
            if (pathSegments.get(i).equals(sourceLanguage)) {
                langIndex = i;
                break;
            }
        }

        if (langIndex == -1) {
            // 可能文件名是语言，例如 en_us.json
            if (originalFileName.startsWith(sourceLanguage + ".")) {
                langIndex = pathSegments.size() - 1;
            } else {
                throw new IllegalArgumentException("源文件路径中未找到语言: " + sourceLanguage);
            }
        }

        // 提取语言文件夹之前的路径部分（%original_path_pre%）
        List<String> preSegments = pathSegments.subList(0, langIndex);
        String originalPathPre = String.join("/", preSegments);

        // 提取语言文件夹之后、文件名之前的路径部分（%original_path%）
        // 注意，由于源语言可能存在于文件名上 (例如：en_us.json)，因此postSegments可能不存在
        String originalPath;
        if (langIndex < pathSegments.size() - 1) {
            List<String> postSegments = pathSegments.subList(langIndex + 1, pathSegments.size() - 1);
            originalPath = String.join("/", postSegments);
        } else {
            originalPath = "";
        }

        // 替换模式中的所有占位符
        String targetPathStr = translationPattern
                .replace("%language%", targetLanguage)
                .replace("%original_path_pre%", originalPathPre)
                .replace("%original_path%", originalPath)
                .replace("%original_file_name%", originalFileName);

        return workspacePath.resolve(targetPathStr);
    }
}
