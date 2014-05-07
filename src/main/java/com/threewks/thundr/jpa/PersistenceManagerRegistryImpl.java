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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.threewks.thundr.jpa.exception.PersistenceManagerDoesNotExistException;

public class PersistenceManagerRegistryImpl implements PersistenceManagerRegistry {
	private ConcurrentMap<String, PersistenceManager> instances = new ConcurrentHashMap<String, PersistenceManager>();

	@Override
	public void register(String persistenceUnit, PersistenceManager persistenceManager) {
		instances.putIfAbsent(persistenceUnit, persistenceManager);
	}

	@Override
	public PersistenceManager get(String persistenceUnit) {
		PersistenceManager persistenceManager = instances.get(persistenceUnit);
		if (persistenceManager == null) {
			throw new PersistenceManagerDoesNotExistException("Persistence manager matching persistence unit %s not found", persistenceUnit);
		}
		return persistenceManager;
	}

	@Override
	public void clear() {
		for (PersistenceManager persistenceManager : instances.values()) {
			persistenceManager.destroy();
		}
		instances.clear();
	}
}
