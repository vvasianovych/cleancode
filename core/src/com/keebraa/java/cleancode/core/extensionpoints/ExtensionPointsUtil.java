package com.keebraa.java.cleancode.core.extensionpoints;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import com.keebraa.java.cleancode.core.exceptions.ChangeSetFactoryNotFoundException;

/**
 * This class provides some methods for work with extension points. Just for
 * simplifying some code.
 * 
 * @author taqi
 * 
 */
public class ExtensionPointsUtil
{
    /**
     * Get unique realization of extension point. Should return [0] element if
     * you will have more then one realization.
     * 
     * @param pointName
     *            - extension point name.
     * @param executableExtensionAttribute
     *            attribute, where contributors will set the name of the
     *            realization.
     * @param realizationType
     *            - type of the realization. May be interface.
     * @return
     * @throws ChangeSetFactoryNotFoundException
     *             if there is no realization, or some problems (will contain
     *             sub-exception inside)
     */
    @SuppressWarnings("unchecked")
    public static <T> T getUniqueExtensionPointRealization(String pointName, String executableExtensionAttribute,
	    Class<T> realizationType) throws ChangeSetFactoryNotFoundException
    {
	T result = null;
	IConfigurationElement[] configElements = Platform.getExtensionRegistry().getConfigurationElementsFor(pointName);
	IConfigurationElement element = configElements[0];
	if (element == null)
	{
	    throw new ChangeSetFactoryNotFoundException();
	}
	try
	{
	    final Object object = element.createExecutableExtension(executableExtensionAttribute);
	    result = (T) object;
	}
	catch (CoreException e)
	{
	    throw new ChangeSetFactoryNotFoundException(e);
	}
	return result;
    }
}
