package com.eastwood.tools.idea;

import com.android.tools.idea.gradle.project.sync.GradleSyncListener;
import com.android.tools.idea.gradle.project.sync.GradleSyncState;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.*;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MicroModuleNotificationProvider extends EditorNotifications.Provider<EditorNotificationPanel> {

    private static final Key<EditorNotificationPanel> KEY = Key.create("MicroModule");

    private final static String MICRO_MODULES = "microModules.xml";
    private final static String IDEA = ".idea";

    @NotNull
    private final Project myProject;
    private boolean enabled;
    private boolean isSync;
    private Map<String, Map<String, MicroModule>> microModuleArtifactState;

    public MicroModuleNotificationProvider(@NotNull Project project) {
        myProject = project;

        VirtualFileManager.getInstance().addVirtualFileListener(new VirtualFileListener() {
            @Override
            public void contentsChanged(@NotNull VirtualFileEvent event) {
                if(MICRO_MODULES.equals(event.getFile().getName())) {
                    if(event.getParent() != null) {
                        if(IDEA.equals(event.getParent().getName())) {
                            refreshMicroModuleArtifactState(event.getFile());
                        }
                    }
                }
            }
        });

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
        if (!enabled || isSync) return null;

        Module module = ProjectFileIndex.getInstance(this.myProject).getModuleForFile(virtualFile);
        if (module == null) return null;

        String modulePath = module.getModuleFile().getParent().getCanonicalPath().replace('\\', '/');
        Map<String, MicroModule> microModuleMap = microModuleArtifactState.get(modulePath);
        if (microModuleMap == null) return null;

        Set<String> paths = microModuleMap.keySet();
        String virtualFilePath = virtualFile.getCanonicalPath().replace('\\', '/');
        for (String path : paths) {
            if (virtualFilePath.contains(path)) {
                MicroModule microModule = microModuleMap.get(path);
                return new StaleRepoSyncNotificationPanel(microModule.name);
            }
        }
        return null;
    }

    private void refreshMicroModuleArtifactState(VirtualFile virtualFile) {
        microModuleArtifactState = new HashMap<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            ByteArrayInputStream is = new ByteArrayInputStream(virtualFile.contentsToByteArray());
            Document doc = builder.parse(is);
            Element rootElement = doc.getDocumentElement();
            NodeList moduleNodeList = rootElement.getElementsByTagName("module");
            for (int i = 0; i < moduleNodeList.getLength(); i++) {
                Element moduleElement = (Element) moduleNodeList.item(i);
                String moduleName = moduleElement.getAttribute("name");
                String modulePath = moduleElement.getAttribute("path").replace('\\', '/');

                Map<String, MicroModule> microModuleMap = new HashMap<>();
                NodeList microModuleNodeList = moduleElement.getElementsByTagName("microModule");
                for (int j = 0; j < microModuleNodeList.getLength(); j++) {
                    Element microModuleElement = (Element) microModuleNodeList.item(j);
                    boolean useMavenArtifact = Boolean.valueOf(microModuleElement.getAttribute("useMavenArtifact"));
                    if (useMavenArtifact) {
                        MicroModule microModule = new MicroModule();
                        microModule.name = microModuleElement.getAttribute("name");
                        microModule.path = microModuleElement.getAttribute("path").replace('\\', '/');
                        microModule.useMavenArtifact = true;
                        microModuleMap.put(microModule.path, microModule);
                    }
                }
                if (!microModuleMap.isEmpty()) {
                    microModuleArtifactState.put(modulePath, microModuleMap);
                }
            }
        } catch (Exception e) {

        }

        enabled = !microModuleArtifactState.isEmpty();
    }

    class StaleRepoSyncNotificationPanel extends EditorNotificationPanel {

        StaleRepoSyncNotificationPanel(String microModuleName) {
            setText("MicroModule '" + microModuleName + "' currently in use maven artifact and should not be edited.");
        }

    }

    class MicroModule {
        String name;
        String path;
        boolean useMavenArtifact;
    }

}
