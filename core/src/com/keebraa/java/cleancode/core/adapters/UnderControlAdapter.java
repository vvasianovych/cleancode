package com.keebraa.java.cleancode.core.adapters;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IActionFilter;

public class UnderControlAdapter implements IAdaptable, IActionFilter
{

    @Override
    public boolean testAttribute(Object target, String name, String value)
    {
	return false;
    }

    @Override
    public Object getAdapter(Class adapter)
    {
	// TODO Auto-generated method stub
	return null;
    }

}
