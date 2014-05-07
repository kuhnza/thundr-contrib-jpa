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

import java.sql.Connection;
import java.sql.SQLException;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.threewks.thundr.action.method.ActionInterceptor;
import com.threewks.thundr.jpa.PersistenceManager;
import com.threewks.thundr.jpa.PersistenceManagerRegistry;
import com.threewks.thundr.jpa.exception.JpaException;
import com.threewks.thundr.logger.Logger;

public class JpaSessionActionInterceptor implements ActionInterceptor<JpaSession> {
	/**
	 * Default transaction isolation level means go with whatever the connection is currently set to.
	 */
	public static int DefaultTransactionIsolation = -1;

	private ThreadLocal<Integer> threadLocalOriginalTransactionIsolation;
	private PersistenceManagerRegistry persistenceManagerRegistry;

	public JpaSessionActionInterceptor(PersistenceManagerRegistry persistenceManagerRegistry) {
		this.persistenceManagerRegistry = persistenceManagerRegistry;
		this.threadLocalOriginalTransactionIsolation = new ThreadLocal<Integer>();
	}

	@Override
	public <T> T before(JpaSession annotation, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
		Logger.debug("Initializing entity manager.");
		PersistenceManager persistenceManager = getPersistenceManager(annotation);

		if (annotation.transactional()) {
			Logger.debug("Configuring transaction isolation level to: %s...", annotation.transactionIsolation());
			configureTransactionIsolation(persistenceManager, annotation.transactionIsolation());
			Logger.debug("Transaction isolation level configured.", annotation.transactionIsolation());

			Logger.debug("Beginning transaction...");
			persistenceManager.beginTransaction();
			Logger.debug("Inside transaction.");
		}
		return null;
	}

	@Override
	public <T> T after(JpaSession annotation, Object view, HttpServletRequest req, HttpServletResponse resp) {
		PersistenceManager persistenceManager = getPersistenceManager(annotation);
		try {
			if (annotation.transactional()) {
				Logger.debug("Committing transaction...");
				persistenceManager.commit();
				Logger.debug("Transaction committed.");
			}
		} finally {
			Logger.debug("Restoring transaction isolation level...");
			restoreDefaultTransactionIsolation(persistenceManager);
			Logger.debug("Transaction isolation level restored.");

			Logger.debug("Closing entity manager...");
			persistenceManager.closeEntityManager();
			Logger.debug("Entity manager closed.");
		}
		return null;
	}

	@Override
	public <T> T exception(JpaSession annotation, Exception e, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
		PersistenceManager persistenceManager = getPersistenceManager(annotation);
		try {
			if (annotation.transactional()) {
				Logger.error("Unchecked exception, rolling back transaction...");
				persistenceManager.rollback();
				Logger.debug("Transaction rolled back.");
			}
		} finally {
			Logger.debug("Restoring transaction isolation level...");
			restoreDefaultTransactionIsolation(persistenceManager);
			Logger.debug("Transaction isolation level restored.");

			Logger.debug("Closing entity manager...");
			persistenceManager.closeEntityManager();
			Logger.debug("Entity manager closed.");
		}
		return null;
	}

	private Connection getConnection(PersistenceManager persistenceManager) {
		EntityManager entityManager = persistenceManager.getEntityManager();
		return entityManager.unwrap(Connection.class);
	}

	private PersistenceManager getPersistenceManager(JpaSession annotation) {
		String persistenceManagerName = annotation.persistenceUnit();
		return persistenceManagerRegistry.get(persistenceManagerName);
	}

	private void configureTransactionIsolation(PersistenceManager persistenceManager, int isolationLevel) {
		try {
			if (isolationLevel != DefaultTransactionIsolation) {
				Connection connection = getConnection(persistenceManager);
				threadLocalOriginalTransactionIsolation.set(connection.getTransactionIsolation());
				connection.setTransactionIsolation(isolationLevel);
			}
		} catch (SQLException e) {
			String message = "Error configuring transaction isolation level: %s";
			Logger.error(message, e.getMessage());
			throw new JpaException(e, message, e.getMessage());
		}
	}

	private void restoreDefaultTransactionIsolation(PersistenceManager persistenceManager) {
		Integer isolationLevel = threadLocalOriginalTransactionIsolation.get();
		if (isolationLevel != null) {
			try {
				Connection connection = getConnection(persistenceManager);
				connection.setTransactionIsolation(isolationLevel);
				threadLocalOriginalTransactionIsolation.set(null);
			} catch (SQLException e) {
				String message = "Error restoring transaction isolation level: %s";
				Logger.error(message, e.getMessage());
				throw new JpaException(e, message, e.getMessage());
			}
		}
	}
}
