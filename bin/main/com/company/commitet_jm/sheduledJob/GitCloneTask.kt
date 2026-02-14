package com.company.commitet_jm.sheduledJob

import com.company.commitet_jm.entity.Project
import com.company.commitet_jm.service.project.ProjectService
import io.jmix.flowui.backgroundtask.BackgroundTask
import io.jmix.flowui.backgroundtask.TaskLifeCycle
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * Фоновая задача для клонирования репозитория проекта
 */
open class GitCloneTask(
    private val projectService: ProjectService,
    private val project: Project
) : BackgroundTask<Int, String>(10, TimeUnit.MINUTES) {

    companion object {
        private val log = LoggerFactory.getLogger(GitCloneTask::class.java)
    }

    /**
     * Результат клонирования
     */
    var resultPath: String? = null
    var errorMessage: String? = null

    @Throws(Exception::class)
    override fun run(taskLifeCycle: TaskLifeCycle<Int>): String {
        log.info("Начало клонирования репозитория для проекта: ${project.name}")

        val result = projectService.cloneProjectRepo(project)

        if (taskLifeCycle.isCancelled) {
            log.info("Задача клонирования отменена")
            return "Отменено"
        }

        if (result.first) {
            resultPath = result.second
            log.info("Репозиторий успешно склонирован: $resultPath")
            return resultPath!!
        } else {
            errorMessage = result.second
            log.error("Ошибка клонирования: $errorMessage")
            throw RuntimeException(errorMessage)
        }
    }
}
