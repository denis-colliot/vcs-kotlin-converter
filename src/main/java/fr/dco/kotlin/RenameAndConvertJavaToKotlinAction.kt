package fr.dco.kotlin

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.actions.VcsContextFactory
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ContentRevision
import com.intellij.openapi.vcs.changes.CurrentContentRevision
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
 * 0. Rollbacking rename action to `.java` extension
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
        private const val JAVA_EXTENSION = ".java"

        /**
         * Kotlin file extension (with separator).
         */
        private const val KOTLIN_EXTENSION = ".kt"

        /**
         * Commit message for the file renaming step.
         */
        private const val COMMIT_MSG = "WIP: Renaming file '%s' with Kotlin extension"
    }

    // -----------------------------------------------------------------------------------------------------------------
    //
    // PLUGIN IMPLEMENTATION.
    //
    // -----------------------------------------------------------------------------------------------------------------

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        selectedJavaFiles(e)
                .filter { it.isWritable }
                .map { it.virtualFile }
                .forEach {
                    // Storing initial Java file content revision (before first rename step).
                    val before = contentRevision(it)

                    // Renaming Java file with Kotlin extension.
                    renameFile(project, it, it.nameWithoutExtension + KOTLIN_EXTENSION)

                    // Committing renaming action into VCS.
                    commit(project, it, before)

                    // Renaming 'Kotlin file' back to Java extension.
                    renameFile(project, it, it.nameWithoutExtension + JAVA_EXTENSION)
                }

        // Invoking native 'Convert Java to Kotlin File' action.
        ActionManager.getInstance().getAction(CONVERT_JAVA_TO_KOTLIN_PLUGIN_ID)?.actionPerformed(e)
    }

    override fun update(e: AnActionEvent) {
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        val project = e.project ?: return
        e.presentation.isEnabled = isAnyJavaFileSelected(project, virtualFiles)
    }

    // -----------------------------------------------------------------------------------------------------------------
    //
    // UTILITY FUNCTIONS.
    //
    // -----------------------------------------------------------------------------------------------------------------

    private fun renameFile(project: Project, virtualFile: VirtualFile, newName: String) {

        logger.info("Renaming file '${virtualFile.name}' to '$newName'")

        WriteCommandAction.runWriteCommandAction(project) {
            try {
                virtualFile.rename(this, newName)
            } catch (e: IOException) {
                throw RuntimeException("Error while renaming file '${virtualFile.name}' to '$newName'", e)
            }
        }
    }

    private fun commit(project: Project, virtualFile: VirtualFile, before: ContentRevision) {

        if (!VcsUtil.isFileUnderVcs(project, virtualFile.path)) {
            logger.info("File '$virtualFile' is not under VCS, aborting commit")
            return
        }

        val after = contentRevision(virtualFile)
        val vcs = VcsUtil.getVcsFor(project, virtualFile)

        vcs?.checkinEnvironment?.commit(listOf(Change(before, after)),
                COMMIT_MSG.format(virtualFile.nameWithoutExtension))
    }

    private fun contentRevision(virtualFile: VirtualFile): CurrentContentRevision {
        val contextFactory = VcsContextFactory.SERVICE.getInstance()
        val path = contextFactory.createFilePathOn(virtualFile)
        return CurrentContentRevision(path)
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
}