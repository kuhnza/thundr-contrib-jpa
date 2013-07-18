/*
 * This file is a component of thundr, a software library from 3wks.
 * Read more: http://www.3wks.com.au/thundr
 * Copyright (C) 2013 3wks, <thundr@3wks.com.au>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.threewks.thundr.jpa.rule;

import com.threewks.thundr.jpa.PersistenceManager;
import org.junit.rules.ExternalResource;

public class SetupTransaction extends ExternalResource {
	public static SetupTransaction commit(PersistenceManager persistenceManager) {
		return new SetupTransaction(persistenceManager, true);
	}

	public static SetupTransaction rollback(PersistenceManager persistenceManager) {
		return new SetupTransaction(persistenceManager, false);
	}

	private PersistenceManager persistenceManager;
	private boolean commit;

	public SetupTransaction(PersistenceManager persistenceManager, boolean commit) {
		this.persistenceManager = persistenceManager;
		this.commit = commit;
	}

	@Override
	protected void before() throws Throwable {
		persistenceManager.beginTransaction();
	}

	@Override
	protected void after() {
		try {
			if (commit) {
				persistenceManager.commit();
			} else {
				persistenceManager.rollback();
			}
		} finally {
			persistenceManager.closeEntityManager();
		}
	}
}
