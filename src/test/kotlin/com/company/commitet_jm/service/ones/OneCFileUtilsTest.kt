package com.company.commitet_jm.service.ones

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

/**
 * Модульные тесты файловых операций 1С. Не требуют Spring-контекста и моков —
 * [OneCFileUtils] не имеет внешних зависимостей.
 */
class OneCFileUtilsTest {

    private val utils = OneCFileUtils()

    @Test
    fun `filterAndRenameFiles keeps and renames target files and deletes the rest`(@TempDir dir: Path) {
        val root = dir.toFile()
        File(root, "form.data").writeText("form")
        File(root, "module.data").writeText("module")
        File(root, "junk.txt").writeText("junk")
        File(root, "nested").mkdir()

        utils.filterAndRenameFiles(
            directory = root,
            keepFiles = setOf("form.data", "module.data"),
            renameRule = { name ->
                when (name) {
                    "form.data" -> "form"
                    "module.data" -> "Module.bsl"
                    else -> name
                }
            }
        )

        assertTrue(File(root, "form").exists(), "form.data должен быть переименован в form")
        assertTrue(File(root, "Module.bsl").exists(), "module.data должен быть переименован в Module.bsl")
        assertFalse(File(root, "form.data").exists(), "исходный form.data не должен остаться")
        assertFalse(File(root, "junk.txt").exists(), "посторонний файл должен быть удалён")
        assertTrue(File(root, "nested").isDirectory, "вложенный каталог должен сохраниться")
    }

    @Test
    fun `clearOrCreateDirectory empties an existing directory but keeps the root`(@TempDir dir: Path) {
        val root = dir.toFile()
        File(root, "a.txt").writeText("a")
        File(root, "b.txt").writeText("b")

        val ok = utils.clearOrCreateDirectory(root.absolutePath, deleteRoot = false)

        assertTrue(ok)
        assertTrue(root.isDirectory, "корневой каталог должен остаться")
        assertTrue(root.listFiles().isNullOrEmpty(), "каталог должен быть пуст")
    }

    @Test
    fun `clearOrCreateDirectory creates a missing directory`(@TempDir dir: Path) {
        val target = File(dir.toFile(), "new-dir")
        assertFalse(target.exists())

        val ok = utils.clearOrCreateDirectory(target.absolutePath, deleteRoot = false)

        assertTrue(ok)
        assertTrue(target.isDirectory)
    }
}
