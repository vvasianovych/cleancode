package com.keebraa.java.cleancode.core.reviewcreation.wizard.pages.commitpage;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Table;

/**
 * This listener listening CommitTable from the first Wizard page and setting
 * complete flag for the page if at least one commit has been selected.
 * 
 * @author taqi
 * 
 */
public class CommitSelectionListener implements SelectionListener
{

    @Override
    public void widgetSelected(SelectionEvent event)
    {
        Object obj = event.getSource();
        if (!(obj instanceof Table))
        {
            return;
        }
        Table table = (Table) obj;
        Object pageObject = table.getData();
        if (!(pageObject instanceof CommitSelectionWizardPage))
        {
            return;
        }
        CommitSelectionWizardPage page = (CommitSelectionWizardPage) pageObject;
        page.setPageComplete(!page.getSelectedCommits().isEmpty());
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent event)
    {
        widgetSelected(event);
    }
}
