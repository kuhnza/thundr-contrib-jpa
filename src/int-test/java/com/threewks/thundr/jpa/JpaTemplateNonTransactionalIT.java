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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.threewks.thundr.jpa.model.Beverage;
import com.threewks.thundr.jpa.rule.SetupPersistenceManager;

public class JpaTemplateNonTransactionalIT {
	@ClassRule
	public static SetupPersistenceManager setupPersistenceManager = new SetupPersistenceManager("test");

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private JpaTemplate<Beverage> template;

	@Before
	public void before() {
		template = new JpaTemplate<Beverage>(setupPersistenceManager.getPersistenceManager(), Beverage.class);

		setupPersistenceManager.getPersistenceManager().beginTransaction();
		template.persist(new Beverage("Coffee", false));
		template.persist(new Beverage("Beer", true));
		setupPersistenceManager.getPersistenceManager().commit();
	}

	@After
	public void after() {
		setupPersistenceManager.getPersistenceManager().beginTransaction();
		for (Beverage beverage : template.query("SELECT b FROM Beverage b")) {
			template.remove(beverage);
		}
		setupPersistenceManager.getPersistenceManager().commit();
	}

	@Test
	public void shouldPermitQueryingOutsideTransaction() {
		long count = template.count();
		assertThat(count, is(2l));

		Beverage retrieved = template.query("SELECT b FROM Beverage b WHERE b.name = 'Coffee'").get(0);
		assertThat(retrieved, is(notNullValue()));
		assertThat(retrieved.getName(), is("Coffee"));
	}
}
