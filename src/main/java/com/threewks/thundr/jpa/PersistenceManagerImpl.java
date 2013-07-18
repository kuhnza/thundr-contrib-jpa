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

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import com.threewks.thundr.jpa.exception.JpaException;
import com.threewks.thundr.logger.Logger;

public class PersistenceManagerImpl implements PersistenceManager {
	private EntityManagerFactory entityManagerFactory;
	private ThreadLocal<EntityManager> threadLocal;

	/**
	 * Only call this if you plan on managing the whole EntityManagerFactory lifecycle yourself. Regular thundr apps
	 * should have no need to create these themselves and should simply declare an injection dependency on
	 * EntityManagerHelper. If you do choose to manage this yourself be sure to call <code>#destroy()</code> to ensure
	 * all resources are properly cleaned up.
	 *
	 * @param persistenceUnit the name of a persistence unit to initialize the EntityManagerFactory with
	 */
	public PersistenceManagerImpl(String persistenceUnit) {
		String className = PersistenceManagerImpl.class.getName();
		try {
			entityManagerFactory = Persistence.createEntityManagerFactory(persistenceUnit);
			threadLocal = new ThreadLocal<EntityManager>();
			Logger.debug("%s initialized.", className);
		} catch (Exception e) {
			Logger.error("Initialization of %s failed: %s", className, e.getMessage());
			throw new JpaException(e, "Initialization of %s failed.", className);
		}
	}

	@Override
	public void destroy() {
		entityManagerFactory.close();
		entityManagerFactory = null;
		threadLocal = null;
		Logger.debug("%s destroyed.", PersistenceManagerImpl.class.getName());
	}

	@Override
	public EntityManager getEntityManager() throws IllegalStateException {
		if (entityManagerFactory == null || !entityManagerFactory.isOpen()) {
			throw new IllegalStateException(
					"EntityManagerFactory is closed. You must now dispose of this instance.");
		}

		try {
			EntityManager em = threadLocal.get();
			if (em == null) {
				em = entityManagerFactory.createEntityManager();
				threadLocal.set(em);
			}
			return em;
		} catch (Exception e) {
			Logger.error("Error creating entity manager: %s", e.getMessage());
			throw new JpaException(e, "Error creating entity manager: %s", e.getMessage());
		}
	}

	@Override
	public void closeEntityManager() {
		EntityManager em = threadLocal.get();
		if (em != null) {
			em.close();
			threadLocal.set(null);
		}
	}

	@Override
	public void beginTransaction() {
		EntityTransaction transaction = getEntityManager().getTransaction();
		if (!transaction.isActive()) {
			transaction.begin();
		}
	}

	@Override
	public void rollback() {
		EntityTransaction transaction = getEntityManager().getTransaction();
		if (transaction.isActive()) {
			transaction.rollback();
		}
	}

	@Override
	public void commit() {
		EntityTransaction transaction = getEntityManager().getTransaction();
		if (transaction.isActive()) {
			transaction.commit();
		}
	}
}
