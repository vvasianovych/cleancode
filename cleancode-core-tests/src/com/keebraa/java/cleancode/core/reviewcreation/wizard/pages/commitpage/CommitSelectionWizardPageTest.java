package com.keebraa.java.cleancode.core.reviewcreation.wizard.pages.commitpage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.junit.Test;
import org.mockito.Mockito;

import com.keebraa.java.cleancode.core.model.Commit;

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

   /**
    * check if page returns right list of checked commits.Two selected.
    */
   @Test
   public void getSelectedCommitsNormalCase()
   {
	CommitTableBuilder builder = mock(CommitTableBuilder.class);
	Table table = mock(Table.class);
	TableItem[] items = new TableItem[3];

	TableItem item1 = mock(TableItem.class);
	Commit commit1 = mock(Commit.class);
	when(item1.getChecked()).thenReturn(true);
	when(item1.getData()).thenReturn(commit1);
	items[0] = item1;
	
	TableItem item2 = mock(TableItem.class);
	Commit commit2 = mock(Commit.class);
	when(item2.getChecked()).thenReturn(true);
	when(item2.getData()).thenReturn(commit2);
	items[1] = item2;
	
	TableItem item3 = mock(TableItem.class);
	when(item3.getChecked()).thenReturn(false);
	items[2] = item3;
	
	when(table.getItems()).thenReturn(items);
	Composite parent = mock(Composite.class);
	when(builder.build()).thenReturn(table);
	CommitSelectionWizardPage page = new CommitSelectionWizardPage(builder);
	page.createControl(parent);
	List<Commit> selectedCommits = page.getSelectedCommits();
	
	assertNotNull(selectedCommits);
	assertEquals(2, selectedCommits.size());
	assertEquals(commit1, selectedCommits.get(0));
	assertEquals(commit2, selectedCommits.get(1));
	
	verify(item1).getData();
	verify(item2).getData();
	verify(item3, Mockito.times(0)).getData();
	verify(table).getItems();
   }
   
   /**
    * Check if page returns not-null after getSelectedCommits
    */
   @Test
   public void getSelectedCommitsNothingSelected()
   {
	CommitTableBuilder builder = mock(CommitTableBuilder.class);
	Table table = mock(Table.class);
	TableItem[] items = new TableItem[3];

	TableItem item1 = mock(TableItem.class);
	when(item1.getChecked()).thenReturn(false);
	items[0] = item1;
	
	TableItem item2 = mock(TableItem.class);
	when(item2.getChecked()).thenReturn(false);
	items[1] = item2;
	
	TableItem item3 = mock(TableItem.class);
	when(item3.getChecked()).thenReturn(false);
	items[2] = item3;
	
	when(table.getItems()).thenReturn(items);
	Composite parent = mock(Composite.class);
	when(builder.build()).thenReturn(table);
	CommitSelectionWizardPage page = new CommitSelectionWizardPage(builder);
	page.createControl(parent);
	List<Commit> selectedCommits = page.getSelectedCommits();
	
	assertNotNull(selectedCommits);
	assertEquals(0, selectedCommits.size());
	
	verify(item1).getChecked();
	verify(item2).getChecked();
	verify(item3).getChecked();
	verify(table).getItems();
   }
}
