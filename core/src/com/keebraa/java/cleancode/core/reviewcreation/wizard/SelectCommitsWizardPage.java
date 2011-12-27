package com.keebraa.java.cleancode.core.reviewcreation.wizard;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;

public class SelectCommitsWizardPage extends WizardPage
{
    private Button button;

    private Table commitTable;
    
    protected SelectCommitsWizardPage(String pageName)
    {
        super(pageName);
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = createContainer(parent);
        GridData gridDataForLabel = new GridData(300, 50);
        Label label1 = new Label(container, SWT.NULL);
        label1.setText("Say hello to Fred");
        label1.setLayoutData(gridDataForLabel);
        commitTable = new Table(container, SWT.BORDER | SWT.SINGLE);
        commitTable.setSize(100, 100);
        GridData gridDataForTable = new GridData(400, 400);
        commitTable.setLayoutData(gridDataForTable);
        setControl(container);
        setPageComplete(false);
        setTitle("commits selection");
        setDescription("Select your commits for code review...");
    }
    
    private Composite createContainer(Composite parent)
    {
        Composite result = new Composite(parent, SWT.NULL);
        GridLayout layout = new GridLayout();
        result.setLayout(layout);
        return result;
    }
}
