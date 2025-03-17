package com.company.commitet_jm.service;

import org.springframework.stereotype.Service
import kotlin.math.log
import org.slf4j.LoggerFactory
import com.company.commitet_jm.entity.Commit
import com.company.commitet_jm.entity.StatusSheduler
import io.jmix.core.DataManager
import java.io.File

@Service
class GitWorker(private val dataManager: DataManager) {
    companion object {
        private  val log = LoggerFactory.getLogger(GitWorker::class.java)
    }


    fun CreateCommit() {

        var commitInfo = firstNewDataCommit()
        //check files repo
        if(commitInfo.project?.localPath?.let { repoIsCloned(it) } == false){
            log.info("Start cloning repo "+ commitInfo.project?.urlRepo)
            commitInfo.project?.urlRepo?.let { commitInfo?.project?.localPath?.let { it1 -> cloneRepo(it, it1) } }
            log.info("Finished cloning repo "+ commitInfo.project?.urlRepo)
        }
        //pull origin
        commitInfo.project?.localPath?.let { commitInfo.project!!.defaultBranch?.let { it1 -> pullOrigin(it, it1) } }
        //create branch
        val branchName = commitInfo.project?.defaultBranch?.let { sanitizeGitBranchName(it) }
        log.info("Start creating branch "+ branchName)
        commitInfo.project?.localPath?.let {
            if (branchName != null) {
                createBranch(it, branchName)
            }else{
                log.error("Branch name is null or empty")
            }
        }
        //save files
//        saveFiles(commitInfo.project?.localPath, commitInfo.files)

        //commit
        //push origin/branch

    }

    fun firstNewDataCommit(): Commit{
        val entity = dataManager.load(Commit::class.java)
        .query("select dCommit from Commit_ dCommit where dCommit.status = :status1 order by dCommit.id asc")
        .parameter("status1", StatusSheduler.NEW)
            .one()
        return entity
    }

    fun repoIsCloned(localPath: String): Boolean{
        val folder = File(localPath)
        return folder.exists()
    }

    fun cloneRepo(url:String, localPath: String){
        val processBuilder = ProcessBuilder("git", "clone", url, localPath)
        processBuilder.start()
    }

    fun pullOrigin(localPath: String, branchName: String){
        val processBuilder = ProcessBuilder("git", "pull", "origin", branchName)
        processBuilder.directory(File(localPath))
        processBuilder.start()
    }

    fun createBranch(localPath: String, branchName: String){
        val processBuilder = ProcessBuilder("git", "branch", branchName)
        processBuilder.directory(File(localPath))
        processBuilder.start()
    }

    fun checkoutBranch(localPath: String, branchName: String){
        val processBuilder = ProcessBuilder("git", "checkout", branchName)
        processBuilder.directory(File(localPath))
        processBuilder.start()
    }

    fun sanitizeGitBranchName(input: String): String {
        // Заменяем все запрещённые символы на "_"
        var sanitized = input.replace(Regex("[^a-zA-Z0-9_.-]"), "_")

        // Удаляем запрещённые символы в начале и конце
        sanitized = sanitized.replace(Regex("^[._-]+"), "")  // начало
        sanitized = sanitized.replace(Regex("[._-]+$"), "")  // конец

        // Заменяем множественные подчёркивания на одно
        sanitized = sanitized.replace(Regex("_+"), "_")

        // Удаляем последовательности из точек/дефисов (например, "..", "--")
        sanitized = sanitized.replace(Regex("[.-]{2,}")) { match -> match.value.first().toString() }

        // Обработка специальных случаев
        return when {
            sanitized.isEmpty() -> "default_branch"
            sanitized == "." || sanitized == ".." -> "branch_${sanitized}"
            else -> sanitized
        }
    }

    fun saveFiles(localPath: String, branchName: String){
        val processBuilder = ProcessBuilder("git", "add", ".")
        processBuilder.directory(File(localPath))
        processBuilder.start()
    }

    fun commit(localPath: String, branchName: String, message: String){
        val processBuilder = ProcessBuilder("git", "commit", "-m", message)
        processBuilder.directory(File(localPath))
        processBuilder.start()
    }
}