package fr.dco.kotlin

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.vcsUtil.VcsUtil
import java.io.IOException
import java.util.*

/**
 * Custom action executing the following steps on each selected file(s):
 *
 * 0. Renaming `.java` file with Kotlin `.kt` extension
 * 0. Committing rename result to VCS
 * 0. Rollback rename action to `.java` extension
 *
 * Once renaming operations are done, the plugin invokes `ConvertJavaToKotlin` native action on the selected files.
 *
 * @see <a href="https://github.com/JetBrains/kotlin/blob/master/idea/src/org/jetbrains/kotlin/idea/actions/JavaToKotlinAction.kt">
 *     Link to 'ConvertJavaToKotlin' official source code</a>
 */
class RenameAndConvertJavaToKotlinAction : AnAction() {

    companion object {

        /**
         * Logger instance.
         */
        private val logger = Logger.getInstance(RenameAndConvertJavaToKotlinAction::class.java)

        /**
         * Official identifier of th native `ConvertJavaToKotlin` action.
         *
         * @see [Source link](https://github.com/JetBrains/kotlin/blob/master/idea/src/META-INF/plugin.xml)
         */
        private const val CONVERT_JAVA_TO_KOTLIN_PLUGIN_ID = "ConvertJavaToKotlin"

        /**
         * Java file extension (with separator).
         */
        private const val JAVA_EXTENSION = "java"

        /**
         * Kotlin file extension (with separator).
         */
        private const val KOTLIN_EXTENSION = "kt"

        /**
         * Commit message for the file renaming step.
         */
        private const val COMMIT_MSG = "WIP: Renaming file '%s' with Kotlin extension"

        /**
         * Storage of the last commit message, in case the user changes it
         */
        private var lastCommitMessage = COMMIT_MSG

    }

    // region Plugin implementation

    override fun actionPerformed(e: AnActionEvent) {

        val project = e.project ?: return

        val selectedJava = selectedJavaFiles(e)
                .filter { it.isWritable }
                .map { it.virtualFile }
                .toList()
                .toTypedArray()

        writeCommitHistory(project, project.baseDir, selectedJava)

        val dataContext = DataContext { data ->
            when (data) {
                PlatformDataKeys.VIRTUAL_FILE_ARRAY.name -> selectedJava
                else -> e.dataContext.getData(data)
            }
        }
        val overrideEvent = AnActionEvent(e.inputEvent, dataContext, e.place, e.presentation, e.actionManager, e.modifiers)

        // Invoking native 'Convert Java to Kotlin File' action.
        ActionManager.getInstance().getAction(CONVERT_JAVA_TO_KOTLIN_PLUGIN_ID)?.actionPerformed(overrideEvent)
    }

    override fun update(e: AnActionEvent) {
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        val project = e.project ?: return
        e.presentation.isEnabled = isAnyJavaFileSelected(project, virtualFiles)
    }

    // endregion

    // region Utility function

    private fun renameFile(project: Project, virtualFile: VirtualFile, newName: String) {

        logger.info("Renaming file `${virtualFile.name}` to `$newName`")

        WriteCommandAction.runWriteCommandAction(project) {
            try {
                virtualFile.rename(this, newName)
            } catch (e: IOException) {
                throw RuntimeException("Error while renaming file `${virtualFile.name}` to `$newName`", e)
            }
        }
    }

    private fun writeCommitHistory(project: Project, projectBase: VirtualFile, files: Array<VirtualFile>): Boolean {
        val commitMessage = Messages.showInputDialog(project, "Commit Message for Conversion:", "Enter a commit message", null, lastCommitMessage, null)
        if (commitMessage.isNullOrBlank()) return false

        lastCommitMessage = commitMessage!!

        val finalVcs = VcsUtil.getVcsFor(project, projectBase) ?: return false
        val changes = files.mapNotNull {
            logger.info("File $it has extension: ${it.extension}")
            if (it.extension != JAVA_EXTENSION) return@mapNotNull null
            val before = it.contentRevision()
            logger.info("Found file ${before.file}")
            renameFile(project, it, "${it.nameWithoutExtension}.$KOTLIN_EXTENSION")
            val after = it.contentRevision()
            logger.info("Renamed file ${before.file} -> ${after.file}")
            Change(before, after)
        }.toList()
        if (changes.isNotEmpty()) {
            finalVcs.checkinEnvironment?.commit(changes, commitMessage)
        } else {
            Messages.showDialog("No files found to commit.", "Nothing to commit", emptyArray(), 0, null)
            logger.info("Cannot commit an empty set of files.")
            return false
        }
        files.forEach {
            if (it.extension != KOTLIN_EXTENSION) return@forEach
            renameFile(project, it, "${it.nameWithoutExtension}.$JAVA_EXTENSION")
        }
        return true
    }

    private fun isAnyJavaFileSelected(project: Project, files: Array<VirtualFile>): Boolean {
        val manager = PsiManager.getInstance(project)

        if (files.any { manager.findFile(it) is PsiJavaFile && it.isWritable }) return true
        return files.any { it.isDirectory && isAnyJavaFileSelected(project, it.children) }
    }

    private fun selectedJavaFiles(e: AnActionEvent): Sequence<PsiJavaFile> {
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return sequenceOf()
        val project = e.project ?: return sequenceOf()
        return allJavaFiles(virtualFiles, project)
    }

    private fun allJavaFiles(filesOrDirs: Array<VirtualFile>, project: Project): Sequence<PsiJavaFile> {
        val manager = PsiManager.getInstance(project)
        return allFiles(filesOrDirs)
                .asSequence()
                .mapNotNull { manager.findFile(it) as? PsiJavaFile }
    }

    private fun allFiles(filesOrDirs: Array<VirtualFile>): Collection<VirtualFile> {
        val result = ArrayList<VirtualFile>()
        for (file in filesOrDirs) {
            VfsUtilCore.visitChildrenRecursively(file, object : VirtualFileVisitor<Unit>() {
                override fun visitFile(file: VirtualFile): Boolean {
                    result.add(file)
                    return true
                }
            })
        }
        return result
    }

    // endregion
}