package com.eastwood.tools.idea.module;

import com.android.tools.adtui.ASGallery;
import com.android.tools.idea.npw.module.ConfigureAndroidModuleStep;
import com.android.tools.idea.npw.module.ModuleGalleryEntry;
import com.android.tools.idea.observable.ListenerManager;
import com.android.tools.idea.observable.ui.TextProperty;
import com.android.tools.idea.wizard.model.ModelWizardStep;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChooseModuleTypeStep extends com.android.tools.idea.npw.module.ChooseModuleTypeStep {

    private final ListenerManager listener;

    private Map<String, String> moduleNameMap;

    public ChooseModuleTypeStep(@NotNull Project project, @NotNull List<ModuleGalleryEntry> moduleGalleryEntries) {
        super(project, moduleGalleryEntries);
        this.listener = new ListenerManager();
        this.moduleNameMap = new HashMap<>();
    }

    @NotNull
    @Override
    public Collection<? extends ModelWizardStep> createDependentSteps() {
        List<ModelWizardStep> allSteps = (List<ModelWizardStep>) super.createDependentSteps();
        for (ModelWizardStep step : allSteps) {
            if (step instanceof ConfigureAndroidModuleStep) {
                ConfigureAndroidModuleStep androidModuleStep = (ConfigureAndroidModuleStep) step;
                Field field = getDeclaredField(androidModuleStep, "myModuleName");
                field.setAccessible(true);
                try {
                    JTextField myModuleName = (JTextField) field.get(androidModuleStep);
                    TextProperty moduleNameText = new TextProperty(myModuleName);
                    this.listener.receive(moduleNameText, (value) -> {
                        moduleNameMap.put(androidModuleStep.getTitle(), value);
                    });
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return allSteps;
    }

    public String getSelectedName() {
        Field field = getDeclaredField(this, "myFormFactorGallery");
        field.setAccessible(true);
        try {
            ASGallery<ModuleGalleryEntry> myFormFactorGallery = (ASGallery<ModuleGalleryEntry>) field.get(this);
            String name = myFormFactorGallery.getSelectedElement().getName();
            return moduleNameMap.get(name);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void dispose() {
        this.listener.releaseAll();
    }

    public static Field getDeclaredField(Object object, String fieldName) {
        Field field = null;
        Class<?> clazz = object.getClass();
        for (; clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
                field = clazz.getDeclaredField(fieldName);
                return field;
            } catch (Exception e) {
            }
        }
        return null;
    }

}
