package com.keebraa.java.cleancode.core.propertytester;

import org.eclipse.core.expressions.PropertyTester;

public class UnderControlPropertyTester extends PropertyTester
{
    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue)
    {
	return true;
    }
}
