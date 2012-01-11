package com.keebraa.java.cleancode.core.reviewcreation.wizard;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.keebraa.java.cleancode.core.exceptions.WizardCommitTableProviderException;
import static com.keebraa.java.cleancode.core.exceptions.WizardCommitTableProviderException.CAST_EXCEPTION;
import com.keebraa.java.cleancode.core.model.Commit;

public class CommitsTableProvider implements IStructuredContentProvider
{
    @Override
    public void dispose()
    {
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
    {

    }

    @Override
    public Object[] getElements(Object inputElement)
    {
	if (!(inputElement instanceof Commit))
	    throw new WizardCommitTableProviderException(CAST_EXCEPTION);
	Commit commit = (Commit) inputElement;
	Object[] data = new Object[3];
	data[0] = false;
	data[1] = commit.getForeignNumber();
	data[2] = commit.getDescription();
	return data;
    }
}
