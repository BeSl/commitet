package com.company.commitet_jm.service.file;

import com.company.commitet_jm.entity.FileCommit;
import com.company.commitet_jm.entity.Platform;
import com.company.commitet_jm.entity.TypesFiles;
import com.company.commitet_jm.component.ShellExecutor;

import java.io.File;
import java.util.List;

public interface FileService {
    void saveFileCommit(String baseDir, List<FileCommit> files, Platform platform);
    File correctPath(String baseDir, TypesFiles type);
    List<File> findBinaryFilesFromGitStatus(String repoDir, ShellExecutor executor);
    void unpackFiles(List<GitWorker.Pair<String, String>> files, Platform platform, ShellExecutor executor, String baseDir);
}