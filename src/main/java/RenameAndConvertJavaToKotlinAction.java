import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.actions.VcsContextFactory;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.changes.CurrentContentRevision;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Stream;

public class RenameAndConvertJavaToKotlinAction extends AnAction {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getInstance(RenameAndConvertJavaToKotlinAction.class);

    /**
     * Identifiant du plugin natif {@code ConvertJavaToKotlin}.
     */
    private static final String CONVERT_JAVA_TO_KOTLIN_PLUGIN_ID = "ConvertJavaToKotlin";

    private AnActionEvent event;
    private Project project;

    public RenameAndConvertJavaToKotlinAction() {
        super();
    }

    @Override
    public void actionPerformed(final AnActionEvent event) {

        this.event = event;
        this.project = event.getProject();

        if (project == null) {
            LOGGER.info("Aucun projet ouvert");
            return;
        }

        final VirtualFile[] selectedFiles = event.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
        if (selectedFiles == null || selectedFiles.length == 0) {
            LOGGER.info("Aucun fichier sélectionné");
            return;
        }

        Stream.of(selectedFiles)
                .filter(Objects::nonNull)
                .forEach(this::transformToKotlin);
    }

    @Override
    public void update(final AnActionEvent e) {
    }

    private void transformToKotlin(@NotNull final VirtualFile virtualFile) {

        if (virtualFile.isDirectory() || !"java".equals(virtualFile.getExtension())) {
            LOGGER.info("Le fichier '" + virtualFile + "' n'est pas un fichier '.java' valide");
            return;
        }

        final ContentRevision before = getContentRevision(virtualFile);

        renameFile(virtualFile, virtualFile.getNameWithoutExtension() + ".kt");

        final ContentRevision after = getContentRevision(virtualFile);

        if (VcsUtil.isFileUnderVcs(project, virtualFile.getPath())) {

            LOGGER.info("Commit du renommage");

            final AbstractVcs vcs = VcsUtil.getVcsFor(project, virtualFile);

            if (vcs == null || vcs.getCheckinEnvironment() == null) {
                LOGGER.info("Aucun contexte VCS de checkin valide pour le fichier '" + virtualFile + "'");
                return;
            }

            vcs.getCheckinEnvironment().commit(Collections.singletonList(new Change(before, after)),
                    "Commit " + virtualFile.getNameWithoutExtension());
        }

        renameFile(virtualFile, virtualFile.getNameWithoutExtension() + ".java");

        // TODO Run default kotlin conversion.
        final AnAction convertJavaToKotlinAction = ActionManager.getInstance().getAction(CONVERT_JAVA_TO_KOTLIN_PLUGIN_ID);
        if (convertJavaToKotlinAction != null) {
            convertJavaToKotlinAction.actionPerformed(event);
        }
    }

    @NotNull
    private CurrentContentRevision getContentRevision(@NotNull final VirtualFile virtualFile) {
        final VcsContextFactory contextFactory = VcsContextFactory.SERVICE.getInstance();
        final FilePath path = contextFactory.createFilePathOn(virtualFile);
        return new CurrentContentRevision(path);
    }

    private void renameFile(@NotNull final VirtualFile virtualFile, @NotNull final String newName) {

        LOGGER.info("Renommage du fichier '" + virtualFile.getName() + "' en '" + newName + "'");

        WriteCommandAction.runWriteCommandAction(project, () -> {
            try {

                virtualFile.rename(this, newName);

            } catch (final IOException e) {
                throw new RuntimeException("Erreur lors du renommage du fichier '" + virtualFile.getName()
                        + "' en '" + newName + "'", e);
            }
        });
    }
}