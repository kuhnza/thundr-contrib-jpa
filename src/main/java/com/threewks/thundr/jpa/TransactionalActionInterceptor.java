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
package com.threewks.thundr.jpa;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import com.threewks.thundr.action.method.ActionInterceptor;
import com.threewks.thundr.logger.Logger;
import com.threewks.thundr.view.ViewResolutionException;

public class TransactionalActionInterceptor implements ActionInterceptor<Transactional> {
	private PersistenceManagerRegistry persistenceManagerRegistry;

	public TransactionalActionInterceptor(PersistenceManagerRegistry persistenceManagerRegistry) {
		this.persistenceManagerRegistry = persistenceManagerRegistry;
	}

	@Override
	public <T> T before(Transactional annotation, HttpServletRequest req, HttpServletResponse resp) {
		Logger.debug("Beginning transaction...");
		getPersistenceManager(annotation).beginTransaction();
		return null;
	}

	@Override
	public <T> T after(Transactional annotation, HttpServletRequest req, HttpServletResponse resp) {
		PersistenceManager persistenceManager = getPersistenceManager(annotation);

		try {
			persistenceManager.commit();
			Logger.debug("Transaction committed.");
		} finally {
			Logger.debug("Closing entity manager");
			persistenceManager.closeEntityManager();
		}

		return null;
	}

	@Override
	public <T> T exception(Transactional annotation, Exception e, HttpServletRequest req, HttpServletResponse resp) {
		PersistenceManager persistenceManager = getPersistenceManager(annotation);

		try {
			Logger.error("Unchecked exception, rolling back transaction.");
			persistenceManager.rollback();
		} finally {
			Logger.debug("Closing entity manager...");
			persistenceManager.closeEntityManager();
			Logger.debug("Entity manager closed.");
		}

		throw new ViewResolutionException(e, e.getMessage());
	}

	private PersistenceManager getPersistenceManager(Transactional annotation) {
		String persistenceManagerName = persistenceManagerName(annotation);
		return persistenceManagerRegistry.get(persistenceManagerName);
	}

	private String persistenceManagerName(Transactional annotation) {
		String persistenceUnit = annotation.value();
		return StringUtils.isNotBlank(persistenceUnit) ? persistenceUnit : PersistenceManager.DefaultName;
	}
}
