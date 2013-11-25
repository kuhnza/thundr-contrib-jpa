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
package com.threewks.thundr.jpa.intercept;


import com.threewks.thundr.action.method.ActionInterceptor;
import com.threewks.thundr.jpa.PersistenceManager;
import com.threewks.thundr.jpa.PersistenceManagerRegistry;
import com.threewks.thundr.logger.Logger;
import com.threewks.thundr.view.ViewResolutionException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DbSessionActionInterceptor implements ActionInterceptor<DbSession> {
	private PersistenceManagerRegistry persistenceManagerRegistry;

	public DbSessionActionInterceptor(PersistenceManagerRegistry persistenceManagerRegistry) {
		this.persistenceManagerRegistry = persistenceManagerRegistry;
	}

	@Override
	public <T> T before(DbSession annotation, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
		Logger.debug("Initializing entity manager.");
		PersistenceManager persistenceManager = getPersistenceManager(annotation);
		if (annotation.transactional()) {
			Logger.debug("Beginning transaction...");
			persistenceManager.beginTransaction();
			Logger.debug("Inside transaction.");
		}
		return null;
	}

	@Override
	public <T> T after(DbSession annotation, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
		PersistenceManager persistenceManager = getPersistenceManager(annotation);
		try {
 			if (annotation.transactional()) {
				Logger.debug("Committing transaction...");
				persistenceManager.commit();
				Logger.debug("Transaction committed.");
			}
		} finally {
			Logger.debug("Closing entity manager...");
			persistenceManager.closeEntityManager();
			Logger.debug("Entity manager closed.");
		}
		return null;
	}

	@Override
	public <T> T exception(DbSession annotation, Exception e, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
		PersistenceManager persistenceManager = getPersistenceManager(annotation);
		try {
			if (annotation.transactional()) {
				Logger.error("Unchecked exception, rolling back transaction...");
				persistenceManager.rollback();
				Logger.debug("Transaction rolled back.");
			}
		} finally {
			Logger.debug("Closing entity manager...");
			persistenceManager.closeEntityManager();
			Logger.debug("Entity manager closed.");
		}
		throw new ViewResolutionException(e, e.getMessage());
	}

	private PersistenceManager getPersistenceManager(DbSession annotation) {
		String persistenceManagerName = annotation.persistenceUnit();
		return persistenceManagerRegistry.get(persistenceManagerName);
	}
}
