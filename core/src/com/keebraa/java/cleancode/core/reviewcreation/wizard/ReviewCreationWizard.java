package com.keebraa.java.cleancode.core.reviewcreation.wizard;

import org.eclipse.jface.wizard.Wizard;

public class ReviewCreationWizard extends Wizard
{
    private static final String WIZARD_TITLE = "Create your code review";

    public ReviewCreationWizard()
    {
	setWindowTitle(WIZARD_TITLE);
    }
    
    @Override
    public boolean performFinish()
    {
	return false;
    }
}
