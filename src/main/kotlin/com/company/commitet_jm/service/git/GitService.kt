package com.company.commitet_jm.service.git

interface GitService {
    fun cloneRepo(repoUrl: String, directoryPath: String, branch: String): Pair<Boolean, String>
    fun createCommit()
}