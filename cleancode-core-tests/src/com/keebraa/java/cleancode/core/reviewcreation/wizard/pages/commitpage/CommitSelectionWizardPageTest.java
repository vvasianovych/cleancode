package com.keebraa.java.cleancode.core.reviewcreation.wizard.pages.commitpage;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.junit.Test;

public class CommitSelectionWizardPageTest
{

   /**
    * Check, if Table has been created and added to the page.
    */
   @Test
   public void isCommitTablePresent()
   {
	CommitTableBuilder builder = mock(CommitTableBuilder.class);
	Table table = mock(Table.class);
	Composite parent = mock(Composite.class);
	when(builder.build()).thenReturn(table);
	CommitSelectionWizardPage page = new CommitSelectionWizardPage(builder);
	page.createControl(parent);
	assertEquals(parent, page.getControl());
	verify(builder).setParent(parent);
	verify(builder).setPage(page);
	verify(builder).build();
   }
}
