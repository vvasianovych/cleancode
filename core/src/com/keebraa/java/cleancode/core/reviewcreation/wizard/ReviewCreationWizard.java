package com.keebraa.java.cleancode.core.reviewcreation.wizard;

import org.eclipse.jface.wizard.Wizard;

public class ReviewCreationWizard extends Wizard
{
    private static final String WIZARD_TITLE = "Create your code review";
    private static final String SELECT_COMMITS_PAGE = "select_commits_page";

    @Override
    public boolean performFinish()
    {
	return false;
    }

    @Override
    public void addPages()
    {
    }
}
