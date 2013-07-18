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


import com.threewks.thundr.jpa.exception.MultiplePersistenceManagersReturnedException;
import com.threewks.thundr.jpa.exception.PersistenceManagerDoesNotExistException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;

public class PersistenceManagerRegistryImplTest {
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private PersistenceManagerRegistry persistenceManagerRegistry;

	@Before
	public void before() {
		persistenceManagerRegistry = new PersistenceManagerRegistryImpl();
	}

	@Test
	public void shouldAddManagerToRegistry() {
		String persistenceUnit = "test";
		PersistenceManager persistenceManager = mock(PersistenceManager.class);

		persistenceManagerRegistry.register(persistenceUnit, persistenceManager);
		assertThat(persistenceManagerRegistry.get(persistenceUnit), is(notNullValue()));
		assertThat(persistenceManagerRegistry.get(persistenceUnit), is(persistenceManager));
	}

	@Test
	public void shouldClearRegistry() {
		thrown.expect(PersistenceManagerDoesNotExistException.class);

		String persistenceUnit = "test";

		persistenceManagerRegistry.register(persistenceUnit, mock(PersistenceManager.class));
		assertThat(persistenceManagerRegistry.get(persistenceUnit), is(notNullValue()));

		persistenceManagerRegistry.clear();
		persistenceManagerRegistry.get(persistenceUnit);
	}

	@Test
	public void shouldRetrieveDefaultWhenOnlyOnePersistenceManagerRegistered() {
		PersistenceManager persistenceManager = mock(PersistenceManager.class);

		persistenceManagerRegistry.register("test", persistenceManager);
		assertThat(persistenceManagerRegistry.get(null), is(notNullValue()));
		assertThat(persistenceManagerRegistry.get(null), is(persistenceManager));
	}

	@Test
	public void shouldThrowErrorRequestingDefaultPersistenceManagerWhenNoneRegistered() {
		thrown.expect(PersistenceManagerDoesNotExistException.class);

		persistenceManagerRegistry.get(null);
	}

	@Test
	public void shouldThrowErrorRequestingDefaultPersistenceManagerWhenMoreThanOneRegistered() {
		thrown.expect(MultiplePersistenceManagersReturnedException.class);

		persistenceManagerRegistry.register("test1", mock(PersistenceManager.class));
		persistenceManagerRegistry.register("test2", mock(PersistenceManager.class));
		persistenceManagerRegistry.get(null);
	}

	@Test
	public void shouldThrowErrorWhenPersistenceUnitNotRegistered() {
		thrown.expect(PersistenceManagerDoesNotExistException.class);

		persistenceManagerRegistry.get("not registered");
	}
}
