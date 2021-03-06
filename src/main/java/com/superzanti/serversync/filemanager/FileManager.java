package com.superzanti.serversync.filemanager;

import com.superzanti.serversync.util.*;
import com.superzanti.serversync.util.enums.EFileMatchingMode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FileManager {
    public final Path configurationFilesDirectory;
    public final Path modFilesDirectory;
    public final Path clientSpecificFilesDirectory;
    public final Path logsDirectory;

    public FileManager() {
        String root = PathUtils.getMinecraftDirectory();

        if (root == null) {
            root = "";
        }

        Logger.debug(String.format("根目录: %s", Paths.get(root).toAbsolutePath().toString()));

        clientSpecificFilesDirectory = new PathBuilder(root).add("clientmods").buildPath();
        modFilesDirectory = new PathBuilder(root).add("mods").buildPath();
        configurationFilesDirectory = new PathBuilder(root).add("config").buildPath();
        logsDirectory = new PathBuilder(root).add("logs").buildPath();
    }

    public ArrayList<SyncFile> getModFiles(
        List<String> includedDirectories,
        EFileMatchingMode fileMatchingMode
    ) {
        return includedDirectories
            .stream()
            .map(Paths::get)
            .filter(path -> {
                // Check for valid include directories
                if (!Files.exists(path)) {
                    Logger.debug(String.format("找不到目录: %s", path.toString()));
                    return false;
                }
                if (!Files.isDirectory(path)) {
                    Logger.debug(String.format("%s 不是一个目录!", path.getFileName()));
                    return false;
                }
                return true;
            })

            .map(dir -> {
                // Get files from valid directories
                try {
                    return PathUtils.fileListDeep(dir);
                } catch (IOException e) {
                    Logger.debug(e);
                    Logger.error("访问文件失败: " + dir.getFileName());
                }
                return new ArrayList<Path>(0);
            })
            .flatMap(ArrayList::stream)
            .filter(file -> {
                if (file == null) {
                    return false;
                }
                // Filter out user ignored files
                if (fileMatchingMode == EFileMatchingMode.NONE) {
                    return true;
                }
                return FileMatcher.shouldIncludeFile(file, fileMatchingMode);
            })
            // Create sync files for the remaining valid list
            .map(SyncFile::StandardSyncFile)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    public ArrayList<SyncFile> getClientOnlyFiles() {
        ArrayList<Path> clientSpecificFiles;
        try {
            clientSpecificFiles = PathUtils.fileListDeep(clientSpecificFilesDirectory);
            return clientSpecificFiles.stream()
                                      .map(SyncFile::ClientOnlySyncFile)
                                      .collect(Collectors.toCollection(ArrayList::new));
        } catch (IOException e) {
            Logger.error("客户端模组未找到");
        }
        return new ArrayList<>(0);
    }

    public ArrayList<SyncFile> getConfigurationFiles(
        List<String> fileMatchPatterns,
        EFileMatchingMode fileMatchingMode
    ) {
        try {
            ArrayList<Path> configFiles = PathUtils.fileListDeep(configurationFilesDirectory);

            Logger.debug("在config中找到  " + configFiles.size() + " 个文件");

            if (fileMatchPatterns != null) {
                Logger.debug("文件匹配模式");
                List<Path> filteredFiles = FileMatcher.filter(configFiles, fileMatchingMode);

                Logger.debug(String.format("需同步的配置文件:  %s", filteredFiles.stream()
                                                                               .map(Path::getFileName)
                                                                               .map(Path::toString)
                                                                               .collect(Collectors.joining(", "))));

                return filteredFiles.stream().map(SyncFile::ConfigSyncFile)
                                    .collect(Collectors.toCollection(ArrayList::new));
            }

            return configFiles.stream().map(SyncFile::ConfigSyncFile).collect(Collectors.toCollection(ArrayList::new));
        } catch (IOException e) {
            Logger.error("访问配置文件失败.");
        }
        return new ArrayList<>(0);
    }
}
