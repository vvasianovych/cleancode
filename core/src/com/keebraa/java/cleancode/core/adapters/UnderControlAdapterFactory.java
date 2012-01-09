package com.keebraa.java.cleancode.core.adapters;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.IActionFilter;

public class UnderControlAdapterFactory implements IAdapterFactory
{

    @Override
    public Object getAdapter(Object adaptableObject, Class adapterType)
    {
	return new UnderControlAdapter();
    }

    @Override
    public Class[] getAdapterList()
    {
	Class[] classes = new Class[1];
	classes[0] = IActionFilter.class;
	return classes;
    }
}
