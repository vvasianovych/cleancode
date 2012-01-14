package com.keebraa.java.cleancode.core.reviewcreation.wizard.commitpage;

import org.eclipse.jface.wizard.IWizardPage;

/**
 * This builder is an "interface" for this package. In a more general
 * understanding. To create page - please, use this builder. The main goals for
 * it - page creation, and also it is used for tests.
 * 
 * @author taqi
 * 
 */
public class CommitSelectionWizardPageBuilder
{
   private CommitTableBuilder commitTableBuilder;

   public void setCommitTableBuilder(CommitTableBuilder commitTableBuilder)
   {
	this.commitTableBuilder = commitTableBuilder;
   }

   public IWizardPage build()
   {
	return new CommitSelectionWizardPage(commitTableBuilder);
   }
}
