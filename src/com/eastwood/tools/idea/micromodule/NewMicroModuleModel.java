package com.eastwood.tools.idea.micromodule;

import com.android.tools.idea.gradle.project.sync.GradleSyncInvoker;
import com.android.tools.idea.observable.core.StringProperty;
import com.android.tools.idea.observable.core.StringValueProperty;
import com.android.tools.idea.wizard.model.WizardModel;
import com.eastwood.tools.idea.MicroModuleService;
import com.eastwood.tools.idea.Utils;
import com.google.wireless.android.sdk.stats.GradleSyncStats;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFileManager;

import java.io.File;
import java.util.List;

public class NewMicroModuleModel extends WizardModel {

    private Module module;
    private File moduleDir;

    private final StringProperty myMicroModuleName = new StringValueProperty();
    private final StringProperty myPackageName = new StringValueProperty();

    private List<String> microModulePackageNames;

    public NewMicroModuleModel(Module module) {
        this.module = module;
        moduleDir = Utils.getModuleDir(module);

        MicroModuleService microModuleService = ServiceManager.getService(module.getProject(), MicroModuleService.class);
        microModulePackageNames = microModuleService.getMicroModulePackageNames(module);
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

    public boolean validatePackageName(String packageName) {
        return microModulePackageNames.contains(packageName);
    }

    @Override
    protected void handleFinished() {
        Utils.createMicroModule(moduleDir, myMicroModuleName.get(), myPackageName.get());
        File buildFile = new File(moduleDir, "build.gradle");
        Utils.includeMicroModule(buildFile, myMicroModuleName.get());
        VirtualFileManager.getInstance().asyncRefresh(new Runnable() {
            @Override
            public void run() {
                GradleSyncInvoker.getInstance().requestProjectSync(module.getProject(), new GradleSyncInvoker.Request(GradleSyncStats.Trigger.TRIGGER_PROJECT_MODIFIED));
            }
        });
    }

}
