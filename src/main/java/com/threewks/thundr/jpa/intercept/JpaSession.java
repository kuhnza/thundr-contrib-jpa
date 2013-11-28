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

import com.threewks.thundr.jpa.PersistenceManager;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = { ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface JpaSession {
	/**
	 * Overrides the default persistence unit name. Defaults to "default".
	 */
	String persistenceUnit() default PersistenceManager.DefaultName;

	/**
	 * Set to <code>true</code> to manage calls inside of a transaction. Defaults to <code>false</code>.
	 */
	boolean transactional() default false;

	/**
	 * Sets the transaction isolation level for the underlying connection. Defaults to -1 which causes
	 * indicates it should use the connection's (i.e the databases' default) isolation level which is
	 * implementation specific.
	 *
	 * @see java.sql.Connection for isolation level constants
	 */
	int transactionIsolation() default -1;
}
