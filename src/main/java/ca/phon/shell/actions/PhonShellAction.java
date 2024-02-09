/*
 * Copyright (C) 2012-2018 Gregory Hedlund & Yvan Rose
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 *    http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.phon.shell.actions;

import java.lang.ref.WeakReference;

import ca.phon.app.hooks.HookableAction;
import ca.phon.shell.PhonShell;

public abstract class PhonShellAction extends HookableAction {

	private static final long serialVersionUID = 4985986930914930136L;

	private final WeakReference<PhonShell> phonShellRef;
	
	public PhonShellAction(PhonShell phonShell) {
		super();
		this.phonShellRef = new WeakReference<PhonShell>(phonShell);
	}
	
	public PhonShell getPhonShell() {
		return phonShellRef.get();
	}
	
}
