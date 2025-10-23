package com.company.commitet_jm.service.file

import com.company.commitet_jm.entity.FileCommit
import com.company.commitet_jm.entity.Platform
import com.company.commitet_jm.entity.TypesFiles
import java.io.File

interface FileService {
    fun saveFileCommit(baseDir: String, files: MutableList<FileCommit>, platform: Platform)
    fun correctPath(baseDir: String, type: TypesFiles): File
    fun findBinaryFilesFromGitStatus(repoDir: String, executor: com.company.commitet_jm.component.ShellExecutor): List<File>
    fun unpackFiles(files: List<Pair<String, String>>, platform: Platform, executor: com.company.commitet_jm.component.ShellExecutor, baseDir: String)
}