package com.keebraa.java.tipstricks;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.keebraa.java.tipstricks.swt.KTable;

public class TestWizardPage extends WizardPage
{

   public TestWizardPage(String pageName)
   {
	super(pageName);
   }

   @Override
   public void createControl(Composite parent)
   {
	new KTable(parent, SWT.NONE);
	setControl(parent);
	setPageComplete(false);
   }
}
