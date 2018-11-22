package com.eastwood.tools.idea;

import com.android.tools.idea.gradle.project.sync.GradleSyncListener;
import com.android.tools.idea.gradle.project.sync.GradleSyncState;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MicroModuleNotificationProvider extends EditorNotifications.Provider<EditorNotificationPanel> {

    private static final Key<EditorNotificationPanel> KEY = Key.create("MicroModule");

    @NotNull
    private final Project myProject;
    private MicroModuleService microModuleService;
    private boolean isSync;

    public MicroModuleNotificationProvider(@NotNull Project project) {
        myProject = project;
        microModuleService = ServiceManager.getService(project, MicroModuleService.class);

        GradleSyncState.subscribe(this.myProject, new GradleSyncListener() {

            @Override
            public void syncStarted(@NotNull Project project, boolean skipped, boolean sourceGenerationRequested) {
                isSync = true;
            }

            @Override
            public void syncSucceeded(@NotNull Project project) {
                isSync = false;
            }

            @Override
            public void syncFailed(@NotNull Project project, @NotNull String errorMessage) {
                isSync = false;
            }

            @Override
            public void syncSkipped(@NotNull Project project) {
                isSync = false;
            }
        });

    }

    @NotNull
    @Override
    public Key<EditorNotificationPanel> getKey() {
        return KEY;
    }

    @Nullable
    @Override
    public EditorNotificationPanel createNotificationPanel(@NotNull VirtualFile virtualFile, @NotNull FileEditor fileEditor) {
        if (isSync) return null;

        Module module = ProjectFileIndex.getInstance(this.myProject).getModuleForFile(virtualFile);
        if (module == null) return null;

        if (virtualFile.getName().endsWith(".gradle")) return null;

        List<MicroModuleInfo> microModuleInfos = microModuleService.getMicroModules(module);
        if (microModuleInfos == null) return null;

        String virtualFilePath = virtualFile.getCanonicalPath().replace('\\', '/');
        for (MicroModuleInfo microModuleInfo : microModuleInfos) {
            if (microModuleInfo.useMavenArtifact) {
                if (virtualFilePath.contains(microModuleInfo.path)) {
                    return new StaleRepoSyncNotificationPanel(microModuleInfo.name);
                }
            }
        }
        return null;
    }

    class StaleRepoSyncNotificationPanel extends EditorNotificationPanel {

        StaleRepoSyncNotificationPanel(String microModuleName) {
            setText("MicroModule '" + microModuleName + "' currently in use maven artifact and should not be edited.");
        }

    }

}
