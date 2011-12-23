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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Put this annotation to the method which shouldn't be shown in the Properties view
 * (per default, all public "is/get/has" methods with zero arguments are shown).
 *
 * @author andrei
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface DoNotDisplayMe {

}
