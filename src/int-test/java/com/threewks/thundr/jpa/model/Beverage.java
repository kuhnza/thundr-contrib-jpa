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
package com.threewks.thundr.jpa.model;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@Table(name = "beverage")
@NamedQueries({ @NamedQuery(name = "Beverage.findAllByName", query = "select b from Beverage b where b.name = ?"),
		@NamedQuery(name = "Beverage.findAllAlcoholicBeverages", query = "select b from Beverage b where b.alcoholic = true"),
		@NamedQuery(name = "Beverage.findAllByType", query = "select b from Beverage b where b.alcoholic = :alcoholic") })
public class Beverage {

	@Id
	@Column(name = "id")
	private String id = UUID.randomUUID().toString();

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "alcoholic")
	private boolean alcoholic = false;

	public Beverage() {

	}

	public Beverage(String name) {
		this.name = name;
	}

	public Beverage(String name, boolean alcoholic) {
		this(name);
		this.alcoholic = alcoholic;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public boolean isAlcoholic() {
		return alcoholic;
	}

	public void setId(String id) {
		this.id = id;
	}

}
