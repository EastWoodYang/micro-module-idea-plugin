package com.eastwood.tools.idea.project;

import com.eastwood.tools.idea.Utils;
import com.android.tools.idea.npw.module.NewModuleModel;
import com.android.tools.idea.npw.project.ConfigureAndroidProjectStep;
import com.android.tools.idea.npw.project.NewProjectModel;
import com.android.tools.idea.sdk.wizard.SdkQuickfixUtils;
import com.android.tools.idea.ui.wizard.StudioWizardDialogBuilder;
import com.android.tools.idea.wizard.model.ModelWizard;
import com.android.tools.idea.wizard.model.ModelWizardStep;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.android.sdk.AndroidSdkUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Iterator;

public class AndroidNewProjectAction extends com.android.tools.idea.actions.AndroidNewProjectAction {

    public AndroidNewProjectAction() {
        this("New Project with MicroModule...");
    }

    public AndroidNewProjectAction(@NotNull String text) {
        super(text);
    }

    public void actionPerformed(AnActionEvent e) {
        if (!AndroidSdkUtils.isAndroidSdkAvailable()) {
            SdkQuickfixUtils.showSdkMissingDialog();
        } else {
            NewProjectModel projectModel = new NewProjectModel();
            ConfigureAndroidProjectStep configureAndroidProjectStep = new ConfigureAndroidProjectStep(projectModel);
            ModelWizard wizard = (new ModelWizard.Builder(new ModelWizardStep[0])).addStep(configureAndroidProjectStep).build();
            wizard.addResultListener(new ModelWizard.WizardListener() {
                @Override
                public void onWizardFinished(@NotNull ModelWizard.WizardResult result) {
                    if (!result.isFinished()) {
                        return;
                    }
                    convertToMicroModule(projectModel);
                }
            });
            (new StudioWizardDialogBuilder(wizard, "Create New Project With MicroModule")).setUseNewUx(true).build().show();
        }
    }


    private void convertToMicroModule(NewProjectModel projectModel) {
        File projectDir = new File(projectModel.projectLocation().get());
        File projectBuild = new File(projectDir, "build.gradle");
        StringBuilder result = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(projectBuild));
            String s = null;
            while ((s = br.readLine()) != null) {
                if (s.contains("classpath 'com.android.tools.build")) {
                    s = s + System.lineSeparator() + "        classpath \"com.eastwood.tools.plugins:micro-module:1.0.0\"";
                }
                result.append(s + System.lineSeparator());
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Utils.write(projectBuild, result.toString());

        Iterator iterator = projectModel.getNewModuleModels().iterator();
        while (iterator.hasNext()) {
            NewModuleModel newModuleModel = (NewModuleModel) iterator.next();
            String moduleName = newModuleModel.moduleName().get();
            File moduleDir = new File(projectDir, moduleName);
            if (!moduleDir.exists()) {
                continue;
            }

            File moduleBuild = new File(moduleDir, "build.gradle");
            if (!moduleBuild.exists()) {
                continue;
            }
            Utils.applyMicroModulePlugin(moduleBuild);
            Utils.moveSrcDir(moduleDir);
        }
    }

}
