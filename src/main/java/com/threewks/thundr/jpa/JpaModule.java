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

import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.commons.lang3.StringUtils;

import com.threewks.thundr.action.method.ActionInterceptorRegistry;
import com.threewks.thundr.configuration.Environment;
import com.threewks.thundr.injection.InjectionContext;
import com.threewks.thundr.injection.Module;
import com.threewks.thundr.injection.UpdatableInjectionContext;
import com.threewks.thundr.jpa.exception.JpaException;
import com.threewks.thundr.jpa.intercept.JpaSession;
import com.threewks.thundr.jpa.intercept.JpaSessionActionInterceptor;
import com.threewks.thundr.logger.Logger;
import com.threewks.thundr.module.DependencyRegistry;

public class JpaModule implements Module {
	public static final String PersistenceManagerRegistry = String.format("thundr-jpa-%s", PersistenceManagerRegistryImpl.class);
	public static final String PersistenceManagersConfigName = "persistenceManagers";

	@Override
	public void requires(DependencyRegistry dependencyRegistry) {
		
	}

	@Override
	public void initialise(UpdatableInjectionContext injectionContext) {
		
	}


	@Override
	public void configure(UpdatableInjectionContext injectionContext) {
		PersistenceManagerRegistry registry = initializePersistenceManagerRegistry(injectionContext);
		registerActionInterceptorAnnotations(injectionContext, registry);
	}
	
	@Override
	public void start(UpdatableInjectionContext injectionContext) {
		
	}

	@Override
	public void stop(InjectionContext injectionContext) {
		PersistenceManagerRegistry registry = injectionContext.get(PersistenceManagerRegistry.class);
		registry.clear();
	}
	

	protected final void registerActionInterceptorAnnotations(UpdatableInjectionContext injectionContext, PersistenceManagerRegistry persistenceManagerRegistry) {
		ActionInterceptorRegistry actionInterceptorRegistry = injectionContext.get(ActionInterceptorRegistry.class);
		actionInterceptorRegistry.registerInterceptor(JpaSession.class, new JpaSessionActionInterceptor(persistenceManagerRegistry));
	}

	protected final PersistenceManagerRegistry initializePersistenceManagerRegistry(UpdatableInjectionContext injectionContext) {
		PersistenceManagerRegistry registry = new PersistenceManagerRegistryImpl();
		injectionContext.inject(registry).as(PersistenceManagerRegistry.class);

		for (Map.Entry<String, String> persistenceManagerAndUnit : getPersistenceUnitNames(injectionContext).entrySet()) {
			String persistenceManagerName = persistenceManagerAndUnit.getKey();
			String persistenceUnitName = persistenceManagerAndUnit.getValue();
			PersistenceManager persistenceManager = new PersistenceManagerImpl(persistenceUnitName);
			registry.register(persistenceManagerName, persistenceManager);
			injectionContext.inject(persistenceManager).named(persistenceManagerName).as(PersistenceManager.class);
			Logger.info("Registered persistence manager %s against persistence unit %s", persistenceManagerName, persistenceUnitName);
		}

		// Put reference into servlet context to provide context listener with access to call
		// PersistenceManagerRegister#clear() on shutdown.
		ServletContext context = injectionContext.get(ServletContext.class);
		context.setAttribute(PersistenceManagerRegistry, registry);

		return registry;
	}

	/**
	 * Gets a list of persistence unit names to initialize. Defaults to a single persistence unit named after the
	 * current environment as provided by {@link Environment#get()}. Override this if you wish to initialize a
	 * different or multiple persistence managers.
	 * 
	 * @param injectionContext
	 * 
	 * @return a list of persistence unit names
	 */
	protected Map<String, String> getPersistenceUnitNames(UpdatableInjectionContext injectionContext) {
		Map<String, String> persistenceManagerAndPersistenceUnit = new LinkedHashMap<String, String>();

		String persistenceManagersString = injectionContext.get(String.class, PersistenceManagersConfigName);
		String[] persistenceManagers = StringUtils.split(persistenceManagersString);
		if (persistenceManagers == null) {
			persistenceManagerAndPersistenceUnit.put(PersistenceManager.DefaultName, PersistenceManager.DefaultName);
		} else {
			for (String persistenceManager : persistenceManagers) {
				String[] persistenceManagerAndUnit = StringUtils.split(persistenceManager, ":");
				if (persistenceManagerAndUnit.length != 2) {
					throw new JpaException(
							"Failed to initialise persistence managers, each persistence manager is expected to be paired with a persistence unit in the form <manager:unit>, but got '%s'",
							persistenceManager);
				}
				persistenceManagerAndPersistenceUnit.put(persistenceManagerAndUnit[0], persistenceManagerAndUnit[1]);
			}
		}
		return persistenceManagerAndPersistenceUnit;
	}
}
