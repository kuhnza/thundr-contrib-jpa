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


import com.google.common.collect.Maps;
import com.threewks.thundr.jpa.exception.MultiplePersistenceManagersReturnedException;
import com.threewks.thundr.jpa.exception.PersistenceManagerDoesNotExistException;

import java.util.concurrent.ConcurrentMap;

public class PersistenceManagerRegistryImpl implements PersistenceManagerRegistry {
	private ConcurrentMap<String, PersistenceManager> instances = Maps.newConcurrentMap();

	@Override
	public void register(String persistenceUnit, PersistenceManager persistenceManager) {
		instances.putIfAbsent(persistenceUnit, persistenceManager);
	}

	@Override
	public PersistenceManager get(String persistenceUnit) {
		if (persistenceUnit == null) {
			return getDefaultPersistenceManager();
		}

		PersistenceManager persistenceManager = instances.get(persistenceUnit);
		if (persistenceManager == null) {
			throw new PersistenceManagerDoesNotExistException(
					"Persistence manager matching persistence unit %s not found", persistenceUnit);
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

	private PersistenceManager getDefaultPersistenceManager() {
		int count = instances.size();

		if (count < 1) {
			throw new PersistenceManagerDoesNotExistException(
					"Unable to retrieve default PersistenceManager. None are registered.");
		} else if (count > 1) {
			throw new MultiplePersistenceManagersReturnedException("Unable to retrieve default PersistenceManager. " +
					"More than one PersistenceManager is registered, you must specify the persistence unit name.");
		}
		return instances.entrySet().iterator().next().getValue();
	}
}
