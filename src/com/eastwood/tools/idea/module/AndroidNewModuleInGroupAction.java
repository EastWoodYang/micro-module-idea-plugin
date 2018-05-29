package com.eastwood.tools.idea.module;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.*;

public class AndroidNewModuleInGroupAction extends AndroidNewModuleAction {

    public AndroidNewModuleInGroupAction() {
        super("Module with MicroModule", "Adds a new module to the project", (Icon) null);
    }

    public void update(AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        Module module = DataKeys.MODULE.getData(dataContext);
        VirtualFile file = DataKeys.VIRTUAL_FILE.getData(dataContext);
        if (module != null && file != null && module.getName().equals(file.getName())) {
            e.getPresentation().setVisible(true);
        } else {
            e.getPresentation().setVisible(false);
        }
    }


}
