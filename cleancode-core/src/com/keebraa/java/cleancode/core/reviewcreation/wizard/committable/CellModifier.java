package com.keebraa.java.cleancode.core.reviewcreation.wizard.committable;

import org.eclipse.jface.viewers.ICellModifier;

public class CellModifier implements ICellModifier
{

    @Override
    public boolean canModify(Object element, String property)
    {
	return true;
    }

    @Override
    public Object getValue(Object element, String property)
    {
	return Boolean.TRUE;
    }

    @Override
    public void modify(Object element, String property, Object value)
    {
	
    }

}
