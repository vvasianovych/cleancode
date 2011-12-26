/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ijuma                 - implementation
 *     Andrei Loskutov       - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.views.console;

import static com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants.PREF_CONSOLE_SHOW_ON_MESSAGE;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleListener;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.themes.ITheme;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

/**
 * This class should only be called from the UI thread as it is not thread-safe.
 */
public final class HgConsoleHolder implements IConsoleListener, IPropertyChangeListener {
	private static final String CONSOLE_FONT = "com.vectrace.mercurialeclipse.ui.colorsandfonts.ConsoleFont"; //$NON-NLS-1$

	private static final HgConsoleHolder INSTANCE = new HgConsoleHolder();

	private volatile HgConsole console;

	private boolean showOnMessage;
	private boolean registered;

	private HgConsoleHolder() {
	}

	public static HgConsoleHolder getInstance() {
		return INSTANCE;
	}

	private void init() {
		if (isInitialized()) {
			return;
		}
		synchronized(this){
			if (isInitialized()) {
				return;
			}
			console = new HgConsole();
			IPreferenceStore store = MercurialEclipsePlugin.getDefault().getPreferenceStore();

			store.addPropertyChangeListener(this);
			getConsoleManager().addConsoleListener(this);

			showOnMessage = store.getBoolean(PREF_CONSOLE_SHOW_ON_MESSAGE);

			// install font
			// see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=298795
			// we must run this stupid code in the UI thread
			if (Display.getCurrent() != null) {
				initUIResources();
			} else {
				PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
					public void run() {
						initUIResources();
					}
				});
			}
		}
	}

	private void initUIResources() {
		ITheme theme = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
		theme.addPropertyChangeListener(this);
		JFaceResources.getFontRegistry().addListener(this);
		setConsoleFont();
	}

	private boolean isInitialized() {
		return console != null;
	}

	public HgConsole showConsole(boolean force) {
		init();

		if (force || showOnMessage) {
			// register console
			if(Display.getCurrent() == null){
				PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
					public void run() {
						registerConsole();
						getConsoleManager().showConsoleView(console);
					}
				});
			} else {
				registerConsole();
				getConsoleManager().showConsoleView(console);
			}
		}

		return console;
	}

	private void registerConsole() {
		boolean exists = isConsoleRegistered();
		if (!exists) {
			getConsoleManager().addConsoles(new IConsole[] { console });
		}
	}

	private boolean isConsoleRegistered() {
		if(registered){
			return true;
		}
		IConsole[] existing = getConsoleManager().getConsoles();
		for (int i = 0; i < existing.length; i++) {
			if (console == existing[i]) {
				registered = true;
			}
		}
		return registered;
	}

	public HgConsole getConsole() {
		init();
		return console;
	}

	public void consolesAdded(IConsole[] consoles) {
		// noop
	}

	public void consolesRemoved(IConsole[] consoles) {
		for (int i = 0; i < consoles.length; i++) {
			IConsole c = consoles[i];
			if (c == console) {
				registered = false;
				console.dispose();
				console = null;
				JFaceResources.getFontRegistry().removeListener(this);
				MercurialEclipsePlugin.getDefault().getPreferenceStore()
						.removePropertyChangeListener(this);
				ITheme theme = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
				theme.removePropertyChangeListener(this);
				break;
			}
		}

	}

	public void propertyChange(PropertyChangeEvent event) {
		if(PREF_CONSOLE_SHOW_ON_MESSAGE.equals(event.getProperty())){
			IPreferenceStore store = MercurialEclipsePlugin.getDefault().getPreferenceStore();
			showOnMessage = store.getBoolean(PREF_CONSOLE_SHOW_ON_MESSAGE);
		} else if (CONSOLE_FONT.equals(event.getProperty())) {
			setConsoleFont();
		} else {
			console.propertyChange(event);
		}
	}

	private IConsoleManager getConsoleManager() {
		return ConsolePlugin.getDefault().getConsoleManager();
	}

	private void setConsoleFont() {
		if (Display.getCurrent() == null) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				public void run() {
					ITheme theme = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
					Font font = theme.getFontRegistry().get(CONSOLE_FONT);
					console.setFont(font);
				}
			});
		} else {
			ITheme theme = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
			Font font = theme.getFontRegistry().get(CONSOLE_FONT);
			console.setFont(font);

		}
	}
}
