package com.keebraa.java.cleancode.core.reviewcreation.wizard.commitpage;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/**
 * This listener listening CommitTable from the first Wizard page and setting
 * complete flag for the page if at least one commit has been selected.
 * 
 * @author taqi
 * 
 */
public class CommitSelectListener implements SelectionListener
{
   private WizardPage page;

   public CommitSelectListener(WizardPage page)
   {
	this.page = page;
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
	boolean selected = false;
	for (TableItem item : table.getItems())
	{
	   if (item.getChecked())
	   {
		selected = true;
		break;
	   }
	}
	page.setPageComplete(selected);
   }

   @Override
   public void widgetDefaultSelected(SelectionEvent event)
   {
	widgetSelected(event);
   }
}
