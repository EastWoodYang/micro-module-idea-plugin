package com.eastwood.tools.idea.micromodule;

import com.android.tools.idea.gradle.project.sync.GradleSyncInvoker;
import com.android.tools.idea.observable.core.StringProperty;
import com.android.tools.idea.observable.core.StringValueProperty;
import com.android.tools.idea.wizard.model.WizardModel;
import com.eastwood.tools.idea.Utils;
import com.intellij.openapi.module.Module;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class NewMicroModuleModel extends WizardModel {

    private Module module;
    private File moduleDir;

    private final StringProperty myMicroModuleName = new StringValueProperty();
    private final StringProperty myPackageName = new StringValueProperty();

    public NewMicroModuleModel(Module module) {
        this.module = module;
        moduleDir = new File(module.getModuleFile().getParent().getPath());
    }

    public StringProperty microModuleName() {
        return myMicroModuleName;
    }

    public StringProperty packageName() {
        return myPackageName;
    }

    public boolean validateMicroModuleName(String name) {
        File microModuleDir = new File(moduleDir, name);
        return microModuleDir.exists();
    }

    @Override
    protected void handleFinished() {
        createMicroModule();
        includeMicroModule();
        module.getProject().getBaseDir().refresh(true, true);
        GradleSyncInvoker.getInstance().requestProjectSync(module.getProject(), GradleSyncInvoker.Request.projectModified());
    }

    private void createMicroModule() {
        File microModuleDir = new File(moduleDir, myMicroModuleName.get());
        microModuleDir.mkdirs();
        File buildFile = new File(microModuleDir, "build.gradle");
        Utils.write(buildFile, "dependencies {\n//    implementation microModule(':you-created-micro-module-name')\n}");

        File srcDir = new File(microModuleDir, "src");
        srcDir.mkdirs();
        String packagePath = myPackageName.get().replace(".", "/");
        String[] types = new String[]{"androidTest", "main", "test"};
        for (String type : types) {
            new File(srcDir, type + File.separator + "java" + File.separator + packagePath).mkdirs();
        }

        File resDir = new File(srcDir, "main/res");
        resDir.mkdir();

        String[] resDirs = new String[]{"drawable", "layouts", "values"};
        for (String type : resDirs) {
            new File(resDir, type).mkdirs();
        }

        File manifestFile = new File(srcDir, "main/AndroidManifest.xml");
        String content = "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n    package=\"" + myPackageName.get() + "\">\n        <application></application>\n</manifest>";
        Utils.write(manifestFile, content);
    }

    private void includeMicroModule() {
        File buildFile = new File(moduleDir, "build.gradle");
        StringBuilder result = new StringBuilder();
        boolean include = false;
        try {
            BufferedReader br = new BufferedReader(new FileReader(buildFile));
            String s = null;
            while ((s = br.readLine()) != null) {
                if (s.contains("microModule")) {
                    String content = s.trim();
                    if (content.startsWith("microModule") && content.endsWith("{")) {
                        include = true;
                        s = s + System.lineSeparator() + "    include ':" + myMicroModuleName.get() + "'";
                    }
                }
                result.append(s + System.lineSeparator());
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!include) {
            result.append("\n");
            result.append("microModule {\n");
            result.append("\n");
            result.append("    include ':" + myMicroModuleName.get() + "'\n");
            result.append("\n");
            result.append("}");
            result.append("\n");
        }
        Utils.write(buildFile, result.toString());

    }

}
