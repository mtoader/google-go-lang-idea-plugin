package ro.redeul.google.go.ide.ui;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import ro.redeul.google.go.ide.GoGlobalConfigurableForm;
import ro.redeul.google.go.ide.GoModuleBuilder;
import ro.redeul.google.go.ide.GoProjectSettings;

import javax.swing.*;

public class GoModuleWizardGlobalSettings extends ModuleWizardStep {
    private GoGlobalConfigurableForm form;
    private GoProjectSettings.GoProjectSettingsBean settingsBean;

    public GoModuleWizardGlobalSettings(GoModuleBuilder moduleBuilder) {
        form = new GoGlobalConfigurableForm();
    }

    @Override
    public JComponent getComponent() {
        return form.componentPanel;
    }

    @Override
    public void updateDataModel() {
        form.apply();
    }
}
