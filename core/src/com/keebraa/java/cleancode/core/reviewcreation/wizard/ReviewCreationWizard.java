package com.keebraa.java.cleancode.core.reviewcreation.wizard;

import org.eclipse.jface.wizard.Wizard;

public class ReviewCreationWizard extends Wizard
{

    @Override
    public boolean performFinish()
    {
        return false;
    }

    @Override
    public void addPages()
    {
        super.addPages();
        SelectCommitsWizardPage page1 = new SelectCommitsWizardPage("select commits...");
        addPage(page1);
    }
}
