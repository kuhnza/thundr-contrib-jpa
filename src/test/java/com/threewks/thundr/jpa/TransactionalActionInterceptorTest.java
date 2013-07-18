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


import com.threewks.thundr.jpa.exception.PersistenceManagerDoesNotExistException;
import com.threewks.thundr.jpa.exception.TransactionalAnnotationException;
import com.threewks.thundr.view.ViewResolutionException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TransactionalActionInterceptorTest {
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private Transactional annotation;
	private PersistenceManagerRegistry persistenceManagerRegistry;
	private PersistenceManager persistenceManager;
	private TransactionalActionInterceptor interceptor;

	@Before
	public void before() {
		annotation = mock(Transactional.class);

		persistenceManager = mock(PersistenceManager.class);

		persistenceManagerRegistry = new PersistenceManagerRegistryImpl();
		persistenceManagerRegistry.register("test", persistenceManager);

		interceptor = new TransactionalActionInterceptor(persistenceManagerRegistry);
	}

	@Test
	public void shouldBeginTransaction() {
		interceptor.before(annotation, null, null);
		verify(persistenceManager).beginTransaction();
	}

	@Test
	public void shouldCommitTransactionAndCloseEntityManager() {
		interceptor.after(annotation, null, null);
		verify(persistenceManager).commit();
		verify(persistenceManager).closeEntityManager();
	}

	@Test
	public void shouldRollbackTransactionAndCloseEntityManager() {
		thrown.expect(ViewResolutionException.class);

		interceptor.exception(annotation, new Exception("Intentional"), null, null);
		verify(persistenceManager).rollback();
		verify(persistenceManager).closeEntityManager();
	}

	@Test
	public void shouldThrowExceptionWhenPassedUnknownPersistenceUnitName() {
		thrown.expect(PersistenceManagerDoesNotExistException.class);

		when(annotation.value()).thenReturn("i don't exist");
		interceptor.before(annotation, null, null);
	}

	@Test
	public void shouldThrowExceptionWhenUsingDefaultPersistenceUnitWithMoreThanOnePersistenceManagerRegistered() {
		thrown.expect(TransactionalAnnotationException.class);

		persistenceManagerRegistry.register("another", mock(PersistenceManager.class));
		interceptor.before(annotation, null, null);
	}
}
