package com.eastwood.tools.idea.micromodule;

import com.android.tools.idea.ui.wizard.StudioWizardDialogBuilder;
import com.android.tools.idea.wizard.model.ModelWizard;
import com.android.tools.idea.wizard.model.ModelWizardStep;
import com.eastwood.tools.idea.Utils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.android.util.AndroidBundle;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class AndroidNewMicroModuleAction extends AnAction {

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        DataContext dataContext = e.getDataContext();
        Module module = DataKeys.MODULE.getData(dataContext);
        VirtualFile file = DataKeys.VIRTUAL_FILE.getData(dataContext);
        if (module != null && module.getModuleFile() != null && file != null) {
            if (module.getModuleFile().getParent().getCanonicalPath().equals(file.getPath())) {
                if (file.getName().equals(e.getProject().getName())) {
                    e.getPresentation().setVisible(false);
                    return;
                }
                File buildFile = new File(file.getPath(), "build.gradle");
                if (buildFile.exists()) {
                    boolean hasAppliedPlugin = false;
                    String content = Utils.read(buildFile);
                    if (content != null) {
                        String[] lines = content.split(System.lineSeparator());
                        for (String line : lines) {
                            line = line.trim();
                            if (line.contains("micro-module") && line.startsWith("apply")) {
                                hasAppliedPlugin = true;
                                break;
                            }
                        }
                    }
                    if (hasAppliedPlugin) {
                        e.getPresentation().setVisible(true);
                        e.getPresentation().setEnabled(true);
                    } else {
                        e.getPresentation().setVisible(false);
                    }
                } else {
                    e.getPresentation().setVisible(false);
                }
            } else {
                e.getPresentation().setVisible(false);
            }
        } else {
            e.getPresentation().setVisible(false);
        }
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        Module module = DataKeys.MODULE.getData(dataContext);
        NewMicroModuleModel microModuleModel = new NewMicroModuleModel(module);
        ConfigMicroModuleStep configMicroModuleStep = new ConfigMicroModuleStep(microModuleModel, "New MicroModule");
        ModelWizard wizard = (new ModelWizard.Builder(new ModelWizardStep[0])).addStep(configMicroModuleStep).build();
        wizard.addResultListener(new ModelWizard.WizardListener() {
            @Override
            public void onWizardFinished(@NotNull ModelWizard.WizardResult result) {
                if (!result.isFinished()) {
                    return;
                }

            }
        });

        (new StudioWizardDialogBuilder(wizard, AndroidBundle.message("android.wizard.module.new.module.title", new Object[0]))).setUseNewUx(true).build().show();
    }

}
