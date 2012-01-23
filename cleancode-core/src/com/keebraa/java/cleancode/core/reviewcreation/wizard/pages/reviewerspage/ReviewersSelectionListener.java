package com.keebraa.java.cleancode.core.reviewcreation.wizard.pages.reviewerspage;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Table;

public class ReviewersSelectionListener implements SelectionListener
{
    @Override
    public void widgetDefaultSelected(SelectionEvent event)
    {
        widgetSelected(event);
    }

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
        if (!(pageObject instanceof ReviewersSelectionWizardPage))
        {
            return;
        }
        ReviewersSelectionWizardPage page = (ReviewersSelectionWizardPage) pageObject;
        page.setPageComplete(!page.getSelectedReviewers().isEmpty());
    }
}
