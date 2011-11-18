/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Andrei Loskutov			- implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.properties;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

/**
 * Allows to show ANY object inside Properties view by introspecting it's public getters.
 *
 * @author andrei
 */
public class GenericPropertySource implements IPropertySource {

	private static final PropPageTitleProvider LABEL_PROVIDER = new PropPageTitleProvider();

	private final Object object;
	private final IPropertyDescriptor[] propertyDescriptors;

	public GenericPropertySource(Object object) {
		this.object = object;
		List<IPropertyDescriptor> props = new ArrayList<IPropertyDescriptor>();
		if (object.getClass().isArray()) {
			Object[] arr = (Object[]) object;
			for (Object aobject : arr) {
				if (aobject != null) {
					props.add(new PropertyDescriptor(aobject, aobject.getClass().getSimpleName()));
				}
			}
		} else if (Collection.class.isAssignableFrom(object.getClass())) {
			Collection<?> arr = (Collection<?>) object;
			for (Object aobject : arr) {
				if (aobject != null) {
					props.add(new PropertyDescriptor(aobject, aobject.getClass().getSimpleName()));
				}
			}
		} else {
			List<Method> getters = getGetters(object);
			for (Method method : getters) {
				Object descriptorId = method;
				// for collections and arrays we want to start another level in the tree
				Class<?> returnType = method.getReturnType();
				if (isArrayOrCollection(returnType)) {
					Object value = getDescriptorFromReturnValue(object, method);
					if(value != null) {
						descriptorId = value;
					}
				}
				props.add(new PropertyDescriptor(descriptorId, getReadableName(method)));
			}
		}
		propertyDescriptors = props.toArray(new PropertyDescriptor[0]);
	}

	public IPropertyDescriptor[] getPropertyDescriptors() {
		return propertyDescriptors;
	}

	public Object getPropertyValue(Object obj) {
		if (obj instanceof Method) {
			try {
				obj = ((Method) obj).invoke(object, (Object[]) null);
			} catch (Exception e) {
				MercurialEclipsePlugin.logError(e);
			}
			if (obj == null) {
				return null;
			}
		}
		if (isArrayOrCollection(obj.getClass())) {
			return new GenericPropertySource(obj);
		}
		if (obj instanceof IPropertySource) {
			return obj;
		}
		Object adapter = Platform.getAdapterManager().getAdapter(obj, IPropertySource.class);
		if (adapter != null) {
			return adapter;
		}
		return "" + obj;
	}

	/**
	 * Overridden to get "value" column filled also for complex types with children
	 * {@inheritDoc}
	 */
	public Object getEditableValue() {
		String text = LABEL_PROVIDER.getText(object);
		return text == null ? "" : text;
	}

	public boolean isPropertySet(Object id) {
		return false;
	}

	public void resetPropertyValue(Object id) {
		//
	}

	public void setPropertyValue(Object id, Object value) {
		//
	}

	private static boolean isArrayOrCollection(Class<?> type) {
		return type.isArray() || Collection.class.isAssignableFrom(type);
	}

	private static String getReadableName(Method method) {
		String name = method.getName();
		return (name.startsWith("get") || name.startsWith("has")) ? name.substring(3) : name
				.startsWith("is") ? name.substring(2) : name;
	}

	private static List<Method> getGetters(Object obj) {
		List<Method> methodList = new ArrayList<Method>();
		Set<String> methodNames = new HashSet<String>();
		Method[] methods = obj.getClass().getMethods();
		for (Method method : methods) {
			if (method.getParameterTypes().length == 0) {
				String name = method.getName();
				if (methodNames.contains(name)) {
					// to prevent overridden methods appear multiple times
					continue;
				}
				if ((name.startsWith("get") || name.startsWith("is") || name.startsWith("has"))
						&& (!name.equals("getClass") && !name.equals("hashCode"))) {
					methodNames.add(name);
					DoNotDisplayMe annotation = method.getAnnotation(DoNotDisplayMe.class);
					if (annotation == null) {
						methodList.add(method);
					}
				}
			}
		}
		return methodList;
	}

	private static Object getDescriptorFromReturnValue(Object instance, Method method) {
		try {
			Object result = method.invoke(instance, (Object[]) null);
			if(result != null) {
				return new GenericPropertySource(result);
			}
		} catch (Exception e) {
			MercurialEclipsePlugin.logError(
					"GenericPropertySource: method invication failed", e);
		}
		return null;
	}
}