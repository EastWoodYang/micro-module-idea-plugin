package com.eastwood.tools.idea;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.*;
import org.jetbrains.android.dom.manifest.Manifest;
import org.jetbrains.android.util.AndroidUtils;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MicroModuleService {

    private final static String MICRO_MODULES = "microModules.xml";
    private final static String IDEA = ".idea";

    private Project project;
    private Map<String, List<MicroModuleInfo>> microModules;
    VirtualFileListener virtualFileListener = new VirtualFileListener() {
        @Override
        public void contentsChanged(@NotNull VirtualFileEvent event) {
            if (project.isDisposed()) {
                VirtualFileManager.getInstance().removeVirtualFileListener(virtualFileListener);
                return;
            }

            if (MICRO_MODULES.equals(event.getFile().getName())) {
                if (event.getParent() != null) {
                    if (IDEA.equals(event.getParent().getName())) {
                        refreshMicroModule();
                    }
                }
            }
        }
    };

    MicroModuleService(Project project) {
        this.project = project;
        microModules = loadMicroModules();

        VirtualFileManager.getInstance().addVirtualFileListener(virtualFileListener);
    }

    public List<MicroModuleInfo> getMicroModules(Module module) {
        File moduleFile = new File(module.getModuleFilePath());
        String modulePath = moduleFile.getParent().replace('\\', '/');
        if (microModules.isEmpty()) {
            microModules = loadMicroModules();
        }
        return microModules.get(modulePath);
    }

    public List<String> getMicroModulePackageNames(Module module) {
        List<String> packageNames = new ArrayList<>();
        List<MicroModuleInfo> microModuleInfoList = getMicroModules(module);
        if (microModuleInfoList == null) {
            return packageNames;
        }

        LocalFileSystem localFileSystem = LocalFileSystem.getInstance();
        for (MicroModuleInfo microModuleInfo : microModuleInfoList) {
            VirtualFile microModuleSrcDir = localFileSystem.findFileByIoFile(new File(microModuleInfo.path, "src"));
            if (microModuleSrcDir == null || !microModuleSrcDir.exists()) continue;
            VirtualFile[] flavorDirs = microModuleSrcDir.getChildren();
            for (VirtualFile flavorDir : flavorDirs) {
                VirtualFile manifestFile = flavorDir.findChild("AndroidManifest.xml");
                if (manifestFile == null || !manifestFile.exists()) continue;

                Manifest manifest = AndroidUtils.loadDomElement(module, manifestFile, Manifest.class);
                if (manifest != null) {
                    String packageName = manifest.getPackage().getValue();
                    if (!StringUtil.isEmptyOrSpaces(packageName) && !packageNames.contains(packageName)) {
                        packageNames.add(packageName);
                    }
                }
            }
        }

        return packageNames;
    }

    private Map<String, List<MicroModuleInfo>> loadMicroModules() {
        Map<String, List<MicroModuleInfo>> microModuleInfoMap = new HashMap<>();
        File microModules = new File(project.getBasePath(), ".idea/microModules.xml");
        VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(microModules);
        if (virtualFile == null || !virtualFile.exists()) return microModuleInfoMap;

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

                List<MicroModuleInfo> microModuleMap = new ArrayList<>();
                NodeList microModuleNodeList = moduleElement.getElementsByTagName("microModule");
                for (int j = 0; j < microModuleNodeList.getLength(); j++) {
                    Element microModuleElement = (Element) microModuleNodeList.item(j);
                    MicroModuleInfo microModule = new MicroModuleInfo();
                    microModule.name = microModuleElement.getAttribute("name");
                    microModule.path = microModuleElement.getAttribute("path").replace('\\', '/');
                    microModuleMap.add(microModule);
                }
                microModuleInfoMap.put(modulePath, microModuleMap);
            }
        } catch (Exception e) {

        }
        return microModuleInfoMap;
    }

    private void refreshMicroModule() {
        File microModuleFile = new File(project.getBasePath(), ".idea/microModules.xml");
        VirtualFile microModuleVirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(microModuleFile);
        if (microModuleVirtualFile == null || !microModuleVirtualFile.exists()) {
            microModules.clear();
            return;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            ByteArrayInputStream is = new ByteArrayInputStream(microModuleVirtualFile.contentsToByteArray());
            Document doc = builder.parse(is);
            Element rootElement = doc.getDocumentElement();
            NodeList moduleNodeList = rootElement.getElementsByTagName("module");
            for (int i = 0; i < moduleNodeList.getLength(); i++) {
                Element moduleElement = (Element) moduleNodeList.item(i);
                String moduleName = moduleElement.getAttribute("name");
                String modulePath = moduleElement.getAttribute("path").replace('\\', '/');

                List<MicroModuleInfo> microModuleList = microModules.get(modulePath);
                if (microModuleList == null) {
                    microModuleList = new ArrayList<>();
                    microModules.put(modulePath, microModuleList);
                }
                microModuleList.clear();

                NodeList microModuleNodeList = moduleElement.getElementsByTagName("microModule");
                for (int j = 0; j < microModuleNodeList.getLength(); j++) {
                    Element microModuleElement = (Element) microModuleNodeList.item(j);
                    MicroModuleInfo microModule = new MicroModuleInfo();
                    microModule.name = microModuleElement.getAttribute("name");
                    microModule.path = microModuleElement.getAttribute("path").replace('\\', '/');
                    microModuleList.add(microModule);
                }
            }
        } catch (Exception e) {

        }
    }

}
