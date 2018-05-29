package com.eastwood.tools.idea.micromodule;

import com.android.tools.adtui.util.FormScalingUtil;
import com.android.tools.adtui.validation.Validator;
import com.android.tools.adtui.validation.ValidatorPanel;
import com.android.tools.idea.observable.ListenerManager;
import com.android.tools.idea.observable.core.ObservableBool;
import com.android.tools.idea.observable.ui.TextProperty;
import com.android.tools.idea.ui.wizard.StudioWizardStepPanel;
import com.android.tools.idea.ui.wizard.WizardUtils;
import com.android.tools.idea.wizard.model.SkippableWizardStep;
import com.google.common.base.CharMatcher;
import org.jetbrains.android.util.AndroidBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class ConfigMicroModuleStep extends SkippableWizardStep<NewMicroModuleModel> {

    private final StudioWizardStepPanel myRootPanel;
    private final ValidatorPanel myValidatorPanel;

    private JPanel myPanel;
    private JTextField myMicroModuleName;
    private JTextField myPackageName;


    private final ListenerManager myListeners;

    public ConfigMicroModuleStep(NewMicroModuleModel model, String title) {
        super(model, title, null);

        myValidatorPanel = new ValidatorPanel(this, myPanel);


        this.myListeners = new ListenerManager();

        TextProperty moduleNameText = new TextProperty(this.myMicroModuleName);
        this.myValidatorPanel.registerValidator(moduleNameText, (value) -> {
            if (value.isEmpty()) {
                return new Validator.Result(Validator.Severity.ERROR, AndroidBundle.message("android.wizard.validate.empty.module.name", new Object[0]));
            } else if (model.validateMicroModuleName(value)) {
                return new Validator.Result(Validator.Severity.ERROR, AndroidBundle.message("android.wizard.validate.module.already.exists", new Object[]{value}));
            } else {
                int illegalCharIdx = CharMatcher.anyOf("[/\\'.").indexIn(value);
                return illegalCharIdx >= 0 ? new Validator.Result(Validator.Severity.ERROR, AndroidBundle.message("android.wizard.validate.module.illegal.character", new Object[]{value.charAt(illegalCharIdx), value})) : Validator.Result.OK;
            }
        });

        TextProperty packageNameText = new TextProperty(this.myPackageName);
        this.myValidatorPanel.registerValidator(packageNameText, (value) -> {
            return Validator.Result.fromNullableMessage(WizardUtils.validatePackageName(value));
        });

        myRootPanel = new StudioWizardStepPanel(myValidatorPanel);
        FormScalingUtil.scaleComponentTree(this.getClass(), myRootPanel);
    }

    @NotNull
    @Override
    protected JComponent getComponent() {
        return myRootPanel;
    }

    protected void onProceeding() {
        NewMicroModuleModel moduleModel = (NewMicroModuleModel) this.getModel();

        moduleModel.microModuleName().set(this.myMicroModuleName.getText());
        moduleModel.packageName().set(this.myPackageName.getText());
    }

    @NotNull
    protected ObservableBool canGoForward() {
        return this.myValidatorPanel.hasErrors().not();
    }

    public void dispose() {
        this.myListeners.releaseAll();
    }

}
