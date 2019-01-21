package com.eastwood.tools.idea.convert;

import com.android.tools.idea.gradle.project.sync.GradleSyncInvoker;
import com.eastwood.tools.idea.Utils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;

public class ConvertToMicroModuleAction extends AnAction {

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        DataContext dataContext = e.getDataContext();
        Module module = DataKeys.MODULE.getData(dataContext);

        VirtualFile file = DataKeys.VIRTUAL_FILE.getData(dataContext);
        if (module != null && file != null && module.getName().equals(file.getName()) && !file.getName().equals(e.getProject().getName())) {
            e.getPresentation().setVisible(true);
            File srcDir = new File(file.getPath(), "src");
            File mainDir = new File(file.getPath(), "main");
            if (srcDir.exists() && !mainDir.exists()) {
                e.getPresentation().setEnabled(true);
            } else {
                e.getPresentation().setEnabled(false);
            }
        } else {
            e.getPresentation().setVisible(false);
        }
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        Module module = DataKeys.MODULE.getData(dataContext);
        File moduleFile = new File(module.getModuleFilePath());
        convertToMicroModule(moduleFile.getParentFile());
        module.getProject().getBaseDir().refresh(true, true);
        GradleSyncInvoker.getInstance().requestProjectSync(module.getProject(), GradleSyncInvoker.Request.projectModified());
    }

    private void convertToMicroModule(File moduleDir) {
        if (!moduleDir.exists()) return;

        File buildFile = new File(moduleDir, "build.gradle");
        if (!buildFile.exists()) return;

        Utils.applyMicroModulePlugin(buildFile);
        Utils.addMicroModuleExtension(buildFile);
        Utils.moveSrcDir(moduleDir);
    }
}
