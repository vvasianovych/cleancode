package com.keebraa.java.cleancode.core.reviewcreation.wizard;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;

public class CodeReviewCreationWizard extends Wizard
{
   private static final String WIZARD_TITLE = "Create your code review";

   public CodeReviewCreationWizard()
   {
	setWindowTitle(WIZARD_TITLE);
   }

   @Override
   public boolean performFinish()
   {
	boolean complete = true;
	for(IWizardPage page : getPages())
	{
	   complete &= page.isPageComplete();
	}
	return complete;
   }
}
