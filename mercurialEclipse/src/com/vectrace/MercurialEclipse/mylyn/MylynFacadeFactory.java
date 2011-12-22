/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * zluspai	implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.mylyn;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

/**
 * Factory for mylyn facade. Used to avoid ClassCastException when mylyn is not available.
 *
 * @author zluspai
 *
 */
public final class MylynFacadeFactory {

	private MylynFacadeFactory() {
		// hide constructor of utility class.
	}

	/**
	 * Get the IMylynFacade instance.
	 * @return The mylyn facade
	 */
	public static IMylynFacade getMylynFacade() {
		Object facade = Proxy.newProxyInstance(MylynFacadeFactory.class.getClassLoader(), new Class[] {IMylynFacade.class}, new InvocationHandler() {

			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				try {
					MylynFacadeImpl impl = new MylynFacadeImpl();
					return method.invoke(impl, args);
				} catch (InvocationTargetException th) {
					// expected if Mylin is not installed => so NO logs here.
				} catch (Throwable t){
					// unexpected => log
					MercurialEclipsePlugin.logError(t);
				}
				return null;
			}
		});
		return (IMylynFacade) facade;
	}

}
