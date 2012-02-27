package com.keebraa.java.cleancode.core.reviewcreation.wizard.pages.reviewerspage;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import com.keebraa.java.cleancode.core.model.Reviewer;
import com.keebraa.java.cleancode.core.model.builders.CodeReviewBuilder;
import com.keebraa.java.cleancode.core.reviewcreation.wizard.pages.CodeReviewCreationWizardPage;

public class ReviewersSelectionWizardPage extends WizardPage implements
        CodeReviewCreationWizardPage
{
    private final static String PAGENAME = "reviewers_selection_wizard_page";

    private final String PAGE_TITLE = "Select reviewers for your code review";

    private final String PAGE_DESCRIPTION = "Please, select reviewers for your code review";

    private List<Reviewer> selectedReviewers;

    private Table reviewerTable;

    private ReviewersTableBuilder tableBuilder;

    public ReviewersSelectionWizardPage(ReviewersTableBuilder tableBuilder)
    {
        super(PAGENAME);
        this.tableBuilder = tableBuilder;
        selectedReviewers = new ArrayList<Reviewer>();
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite myParent = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout();
        myParent.setLayout(gl);
        setTitle(PAGE_TITLE);
        setDescription(PAGE_DESCRIPTION);
        tableBuilder.setParent(myParent);
        tableBuilder.setPage(this);
        reviewerTable = tableBuilder.build();
        GridData data = new GridData(GridData.FILL, GridData.FILL, true, true);
        reviewerTable.setLayoutData(data);
        setControl(myParent);
        setPageComplete(false);
    }

    @Override
    public void fillCodeReviewBuilder(CodeReviewBuilder builder)
    {
        builder.setReviewes(selectedReviewers);
    }

    public List<Reviewer> getSelectedReviewers()
    {
        selectedReviewers.clear();
        for (TableItem item : reviewerTable.getItems())
        {
            if (!item.getChecked())
            {
                continue;
            }
            Reviewer reviewer = (Reviewer) item.getData();
            selectedReviewers.add(reviewer);
        }
        return selectedReviewers;
    }
}
