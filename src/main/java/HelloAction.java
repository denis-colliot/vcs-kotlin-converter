import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.actions.VcsContextFactory;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.CurrentContentRevision;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.ArrayList;
import java.util.List;

public class HelloAction extends AnAction {

    public HelloAction() {
        super("Hello");
    }

    public void actionPerformed(final AnActionEvent event) {

        final Project project = event.getProject();
        if (project == null) {
            return;
        }

        final VirtualFile virtualFile = event.getDataContext().getData(PlatformDataKeys.VIRTUAL_FILE);

        final ProjectLevelVcsManager plvm = ProjectLevelVcsManager.getInstance(project);
        final AbstractVcs vcs = plvm.getVcsFor(virtualFile);

        final VcsContextFactory contextFactory = VcsContextFactory.SERVICE.getInstance();
        final FilePath path = contextFactory.createFilePathOn(virtualFile);
        final CurrentContentRevision cr = new CurrentContentRevision(path);

        final List<Change> changes = new ArrayList<>();
        final Change c = new Change(cr, cr);
        changes.add(c);
        vcs.getCheckinEnvironment().commit(changes, "Commit " + virtualFile.getNameWithoutExtension());

        Messages.showMessageDialog(project,
                "Hello world!",
                "Greeting",
                Messages.getInformationIcon());
    }
}