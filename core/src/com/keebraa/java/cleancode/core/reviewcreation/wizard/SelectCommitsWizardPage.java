package com.keebraa.java.cleancode.core.reviewcreation.wizard;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;

public class SelectCommitsWizardPage extends WizardPage
{
    private Button button;

    private Table commitTable;
    
    private Composite container;
    
    protected SelectCommitsWizardPage(String pageName)
    {
        super(pageName);
    }

    @Override
    public void createControl(Composite parent)
    {
        container = new Composite(parent, SWT.NULL);
        GridLayout layout = new GridLayout();
        container.setLayout(layout);
        Label label1 = new Label(container, SWT.NULL);
        label1.setText("Say hello to Fred");
        commitTable = new Table(container, SWT.BORDER | SWT.SINGLE);
        commitTable.setSize(100, 100);
        setControl(container);
        setPageComplete(false);
    }
}
