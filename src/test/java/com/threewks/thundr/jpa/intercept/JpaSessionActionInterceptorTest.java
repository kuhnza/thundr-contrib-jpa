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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.threewks.thundr.jpa.PersistenceManager;
import com.threewks.thundr.jpa.PersistenceManagerImpl;
import com.threewks.thundr.jpa.PersistenceManagerRegistry;
import com.threewks.thundr.jpa.PersistenceManagerRegistryImpl;
import com.threewks.thundr.jpa.exception.PersistenceManagerDoesNotExistException;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Persistence.class)
public class JpaSessionActionInterceptorTest {
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private Connection connection;
	private JpaSession annotation;
	private PersistenceManager persistenceManager;
	private JpaSessionActionInterceptor interceptor;

	@SuppressWarnings("unchecked")
	@Before
	public void before() {
		annotation = spy(new MockJpaSessionAnnotation());

		EntityManager entityManager = mock(EntityManager.class);
		when(entityManager.getTransaction()).thenReturn(mock(EntityTransaction.class));

		connection = mock(Connection.class);
		doReturn(connection).when(entityManager).unwrap(any(Class.class));

		EntityManagerFactory entityManagerFactory = mock(EntityManagerFactory.class);
		when(entityManagerFactory.isOpen()).thenReturn(true);
		when(entityManagerFactory.createEntityManager()).thenReturn(entityManager);

		PowerMockito.mockStatic(Persistence.class);
		when(Persistence.createEntityManagerFactory(Mockito.anyString())).thenReturn(entityManagerFactory);

		persistenceManager = spy(new PersistenceManagerImpl("default"));
		PersistenceManagerRegistry persistenceManagerRegistry = new PersistenceManagerRegistryImpl();
		persistenceManagerRegistry.register("default", persistenceManager);

		interceptor = new JpaSessionActionInterceptor(persistenceManagerRegistry);
	}

	@Test
	public void shouldBeginTransaction() {
		when(annotation.transactional()).thenReturn(true);

		interceptor.before(annotation, null, null);
		verify(persistenceManager).beginTransaction();
	}

	@Test
	public void shouldNotBeginTransactionWhenNoTransaction() {
		when(annotation.transactional()).thenReturn(false);

		interceptor.before(annotation, null, null);
		verify(persistenceManager, times(0)).beginTransaction();
	}

	@Test
	public void shouldCommitTransactionAndCloseEntityManager() {
		when(annotation.transactional()).thenReturn(true);

		interceptor.after(annotation, null, null, null);
		verify(persistenceManager).commit();
		verify(persistenceManager).closeEntityManager();
	}

	@Test
	public void shouldNotAttemptToCommitWhenNoTransaction() {
		when(annotation.transactional()).thenReturn(false);

		interceptor.after(annotation, null, null, null);
		verify(persistenceManager, times(0)).commit();
		verify(persistenceManager).closeEntityManager();
	}

	@Test
	public void shouldAlwaysCloseEntityManagerOnCommitEvenOnExceptions() {
		when(annotation.transactional()).thenReturn(true);

		doThrow(new RuntimeException("expected")).when(persistenceManager).commit();
		try {
			interceptor.after(annotation,  null, null, null);
			fail("Expected exception");
		} catch (RuntimeException e) {
			assertThat(e.getMessage(), is("expected"));
		}

		verify(persistenceManager).closeEntityManager();
	}

	@Test
	public void shouldRollbackTransactionAndCloseEntityManager() {
		when(annotation.transactional()).thenReturn(true);

		Object view = interceptor.exception(annotation, new Exception("Intentional"), null, null);
		assertThat(view, is(nullValue()));
		verify(persistenceManager).rollback();
		verify(persistenceManager).closeEntityManager();
	}

	@Test
	public void shouldAlwaysCloseEntityManagerOnRollbackEvenOnExceptions() {
		when(annotation.transactional()).thenReturn(true);

		doThrow(new RuntimeException("expected")).when(persistenceManager).rollback();
		try {
			RuntimeException e = new RuntimeException("cause");
			interceptor.exception(annotation, e, null, null);
			fail("Expected exception");
		} catch (RuntimeException e) {
			assertThat(e.getMessage(), is("expected"));
		}

		verify(persistenceManager).closeEntityManager();
	}

	@Test
	public void shouldThrowExceptionWhenPassedUnknownPersistenceUnitName() {
		thrown.expect(PersistenceManagerDoesNotExistException.class);

		when(annotation.persistenceUnit()).thenReturn("i don't exist");
		interceptor.before(annotation, null, null);
	}

	@Test
	public void shouldConfigureAlternateTransactionIsolationThenRestoreDefaultTransactionIsolation() throws SQLException {
		int defaultIsolationLevel = Connection.TRANSACTION_NONE;
		int temporaryIsolationLevel = Connection.TRANSACTION_REPEATABLE_READ;

		when(annotation.transactional()).thenReturn(true);
		when(annotation.transactionIsolation()).thenReturn(temporaryIsolationLevel);
		when(connection.getTransactionIsolation()).thenReturn(defaultIsolationLevel);

		interceptor.before(annotation, null, null);
		interceptor.after(annotation,  null, null, null);

		ArgumentCaptor<Integer> argument = ArgumentCaptor.forClass(Integer.class);
		verify(connection, times(2)).setTransactionIsolation(argument.capture());
		List<Integer> values = argument.getAllValues();
		assertThat(values.size(), is(2));
		assertThat(values.get(0), is(temporaryIsolationLevel));
		assertThat(values.get(1), is(defaultIsolationLevel));
	}

	@Test
	public void shouldConfigureAlternateTransactionIsolationThenRestoreDefaultTransactionIsolationOnException() throws SQLException {
		int defaultIsolationLevel = Connection.TRANSACTION_NONE;
		int temporaryIsolationLevel = Connection.TRANSACTION_REPEATABLE_READ;

		when(annotation.transactional()).thenReturn(true);
		when(annotation.transactionIsolation()).thenReturn(temporaryIsolationLevel);
		when(connection.getTransactionIsolation()).thenReturn(defaultIsolationLevel);

		interceptor.before(annotation, null, null);
		Object view = interceptor.exception(annotation, new Exception("Expected"), null, null);
		assertThat(view, is(nullValue()));

		ArgumentCaptor<Integer> argument = ArgumentCaptor.forClass(Integer.class);
		verify(connection, times(2)).setTransactionIsolation(argument.capture());
		List<Integer> values = argument.getAllValues();
		assertThat(values.size(), is(2));
		assertThat(values.get(0), is(temporaryIsolationLevel));
		assertThat(values.get(1), is(defaultIsolationLevel));
	}
}
