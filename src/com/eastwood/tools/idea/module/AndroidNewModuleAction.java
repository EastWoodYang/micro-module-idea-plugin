package com.eastwood.tools.idea.module;

import com.android.tools.idea.npw.module.ModuleDescriptionProvider;
import com.android.tools.idea.npw.module.ModuleGalleryEntry;
import com.android.tools.idea.projectsystem.ProjectSystemUtil;
import com.android.tools.idea.sdk.wizard.SdkQuickfixUtils;
import com.android.tools.idea.ui.wizard.StudioWizardDialogBuilder;
import com.android.tools.idea.wizard.model.ModelWizard;
import com.android.tools.idea.wizard.model.ModelWizardStep;
import com.eastwood.tools.idea.Utils;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.android.sdk.AndroidSdkUtils;
import org.jetbrains.android.util.AndroidBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;

public class AndroidNewModuleAction extends com.android.tools.idea.actions.AndroidNewModuleAction {

    public AndroidNewModuleAction() {

    }

    public AndroidNewModuleAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
        super(text, description, icon);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project != null && ProjectSystemUtil.getProjectSystem(project).allowsFileCreation()) {
            if (!AndroidSdkUtils.isAndroidSdkAvailable()) {
                SdkQuickfixUtils.showSdkMissingDialog();
                return;
            }

            ArrayList<ModuleGalleryEntry> moduleDescriptions = new ArrayList();
            ModuleDescriptionProvider[] var4 = (ModuleDescriptionProvider[]) ModuleDescriptionProvider.EP_NAME.getExtensions();
            int var5 = var4.length;

            for (int var6 = 0; var6 < var5; ++var6) {
                ModuleDescriptionProvider provider = var4[var6];
                moduleDescriptions.addAll(provider.getDescriptions());
            }

            ChooseModuleTypeStep chooseModuleTypeStep = new ChooseModuleTypeStep(project, moduleDescriptions);
            ModelWizard wizard = (new ModelWizard.Builder(new ModelWizardStep[0])).addStep(chooseModuleTypeStep).build();
            wizard.addResultListener(new ModelWizard.WizardListener() {
                @Override
                public void onWizardFinished(@NotNull ModelWizard.WizardResult result) {
                    if (!result.isFinished()) {
                        return;
                    }

                    String newModuleName = chooseModuleTypeStep.getSelectedName();
                    if (newModuleName == null) return;

                    File moduleDir = new File(project.getBasePath(), newModuleName);
                    convertToMicroModule(moduleDir);

                }
            });

            (new StudioWizardDialogBuilder(wizard, AndroidBundle.message("android.wizard.module.new.module.title", new Object[0]))).setUxStyle(StudioWizardDialogBuilder.UxStyle.INSTANT_APP).build().show();
        }
    }

    private void convertToMicroModule(File moduleDir) {
        if (!moduleDir.exists()) return;

        File buildFile = new File(moduleDir, "build.gradle");
        Utils.applyMicroModulePlugin(buildFile);
        Utils.moveSrcDir(moduleDir);
    }


}
