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

import com.threewks.thundr.jpa.exception.JpaException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.persistence.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Persistence.class)
public class PersistenceManagerImplTest {
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private PersistenceManagerImpl persistenceManager;
	private EntityManager entityManager;
	private EntityTransaction transaction;
	private EntityManagerFactory entityManagerFactory;

	@Before
	public void before() {
		transaction = mock(EntityTransaction.class);

		EntityManager entityManager = mock(EntityManager.class);
		when(entityManager.getTransaction()).thenReturn(transaction);

		entityManagerFactory = mock(EntityManagerFactory.class);
		when(entityManagerFactory.isOpen()).thenReturn(true);
		when(entityManagerFactory.createEntityManager()).thenReturn(entityManager);

		PowerMockito.mockStatic(Persistence.class);
		when(Persistence.createEntityManagerFactory(Mockito.anyString())).thenReturn(entityManagerFactory);

		persistenceManager = new PersistenceManagerImpl("test");
		this.entityManager = persistenceManager.getEntityManager();
	}

	@Test
	public void shouldReturnSameEntityManagerOnSameThread() {
		EntityManager entityManager = persistenceManager.getEntityManager();
		assertThat(entityManager, is(notNullValue()));
		assertThat(persistenceManager.getEntityManager(), is(entityManager));
		assertThat(persistenceManager.getEntityManager(), is(this.entityManager));
	}

	@Test
	public void shouldBeginTransaction() {
		persistenceManager.beginTransaction();
		verify(transaction).begin();
	}

	@Test
	public void shouldCommitTransaction() {
		when(transaction.isActive()).thenReturn(true);

		persistenceManager.commit();
		verify(transaction).commit();
	}

	@Test
	public void shouldRollbackTransaction() {
		when(transaction.isActive()).thenReturn(true);

		persistenceManager.rollback();
		verify(transaction).rollback();
	}

	@Test
	public void shouldCloseEntityManager() {
		persistenceManager.closeEntityManager();
		verify(entityManager).close();
	}

	@Test
	public void shouldCloseEntityManagerFactory() {
		persistenceManager.destroy();
		verify(entityManagerFactory).close();
	}

	@Test
	public void shouldThrowExceptionOnAccessIfPersistenceManagerDestroyed() {
		thrown.expect(IllegalStateException.class);

		persistenceManager.destroy();
		persistenceManager.getEntityManager();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void shouldThrowExceptionIfErrorOnInitialization() {
		thrown.expect(JpaException.class);

		when(Persistence.createEntityManagerFactory(Mockito.anyString())).thenThrow(PersistenceException.class);
		new PersistenceManagerImpl("test");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void shouldThrowExceptionIfErrorCreatingEntityManager() {
		thrown.expect(JpaException.class);

		when(entityManagerFactory.createEntityManager()).thenThrow(PersistenceException.class);
		persistenceManager.closeEntityManager();
		persistenceManager.getEntityManager();
	}
}
