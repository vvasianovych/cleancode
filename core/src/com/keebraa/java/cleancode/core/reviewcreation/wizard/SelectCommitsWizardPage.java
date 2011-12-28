package com.keebraa.java.cleancode.core.reviewcreation.wizard;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Table;

public class SelectCommitsWizardPage extends WizardPage
{
    private static final String PAGE_TITLE = "Select your commits";

    private static final String PAGE_DESCRIPTION = "Please, select changes for code review";

    private Table commitTable;
    
    private Label someText;
    
    private Layout layout;

    protected SelectCommitsWizardPage(String pageName)
    {
	super(pageName);
    }

    @Override
    public void createControl(Composite parent)
    {
	setTitle(PAGE_TITLE);
	setDescription(PAGE_DESCRIPTION);
	Composite container = createContainer(parent);
	container.setFocus();
	FormData formData = new FormData();
	formData.left = new FormAttachment(0, 0);
	formData.top = new FormAttachment(0, 0);
	formData.right = new FormAttachment(100, 0);
	formData.bottom = new FormAttachment(100, 0);
	Composite commitTable = getCommitTable(container, formData);
//	FormData dataForText = new FormData();
//	dataForText.left = new FormAttachment(0, 0);
//	dataForText.top = new FormAttachment(commitTable, 5);
//	getText(container, dataForText);
	setControl(container);
	setPageComplete(false);
    }
    
    private Composite createContainer(Composite parent)
    {
	Composite result = new Composite(parent, SWT.BORDER);
	Layout layout = createLayout();
	result.setLayout(layout);
	return result;
    }

    private Layout createLayout()
    {
	if(layout == null)
	{
	    FormLayout formLayout = new FormLayout();
	    layout = formLayout;
	}
	return layout;
    }
    private Composite getCommitTable(Composite parent, Object layoutData)
    {
	if (commitTable == null)
	{
	    commitTable = new Table(parent, SWT.NONE);
	    commitTable.setLayoutData(layoutData);
	}
	return commitTable;
    }
    
    private Label getText(Composite parent, Object layoutData)
    {
	if(someText == null)
	{
	    someText = new Label(parent, SWT.BORDER);
	    someText.setText("fuck up!");
	    someText.setLayoutData(layoutData);
	}
	return someText;
    }
}
