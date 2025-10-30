package com.company.commitet_jm.service.ones;

import java.io.File;

public interface OneCService {
    void uploadExtFiles(File inputFile, String outDir, String pathInstall, String version);
    void unpackExtFiles(File inputFile, String outDir);
    String pathPlatform(String basePath, String version);
}