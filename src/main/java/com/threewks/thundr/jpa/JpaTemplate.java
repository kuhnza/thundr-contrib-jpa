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

import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

/**
 * JpaTemplate class inspired by Spring's JpaTemplate without Spring framework baggage.
 */
public class JpaTemplate<T> {

	private PersistenceManager persistenceManager;
	private Class<T> type;

	public JpaTemplate(PersistenceManager persistenceManager, Class<T> type) {
		this.persistenceManager = persistenceManager;
		this.type = type;
	}

	public EntityManager getEntityManager() {
		return persistenceManager.getEntityManager();
	}

	public <E> E execute(JpaAction<E> action) {
		EntityManager em = getEntityManager();
		return action.run(em);
	}

	public List<T> executeFind(JpaAction<List<T>> action) {
		return execute(action);
	}

	public long count() {
		return execute(new JpaAction<Long>() {
			@Override
			public Long run(EntityManager em) {
				return (Long) em.createQuery("SELECT count(*) FROM " + type.getName()).getSingleResult();
			}
		});
	}

	public T get(final Object id) {
		return execute(new JpaAction<T>() {
			@Override
			public T run(EntityManager em) {
				return em.find(type, id);
			}
		});
	}

	public T getReference(final Object id) {
		return execute(new JpaAction<T>() {
			@Override
			public T run(EntityManager em) {
				// TODO - the api contract of throwing EntityNotFoundException is unclear, this should probably be consistent from this method
				T reference = em.getReference(type, id);
				return reference;
			}
		});
	}

	public boolean contains(final T entity) {
		return execute(new JpaAction<Boolean>() {
			@Override
			public Boolean run(EntityManager em) {
				return em.contains(entity);
			}
		});
	}

	public T merge(final T entity) {
		return execute(new JpaAction<T>() {
			@Override
			public T run(EntityManager em) {
				return em.merge(entity);
			}
		});
	}

	public void persist(final T entity) {
		execute(new VoidAction() {
			@Override
			public void vrun(EntityManager em) {
				em.persist(entity);
			}
		});
	}

	public void remove(final T entity) {
		execute(new VoidAction() {
			@Override
			public void vrun(EntityManager em) {
				em.remove(entity);
			}
		});
	}

	public void refresh(final T entity) {
		execute(new VoidAction() {
			@Override
			public void vrun(EntityManager em) {
				em.refresh(entity);
			}
		});
	}

	public void flush() {
		execute(new VoidAction() {
			@Override
			public void vrun(EntityManager em) {
				em.flush();
			}
		});
	}

	public List<T> query(String query) {
		return query(query, (Object[]) null);
	}

	public List<T> query(final String query, final Object... values) {
		return executeFind(new JpaAction<List<T>>() {
			@Override
			public List<T> run(EntityManager em) {
				TypedQuery<T> queryObject = em.createQuery(query, type);
				if (values != null) {
					for (int i = 0; i < values.length; i++) {
						queryObject.setParameter(i + 1, values[i]);
					}
				}
				return queryObject.getResultList();
			}
		});
	}

	public List<T> query(final String query, final Map<String, Object> params) {
		return executeFind(new JpaAction<List<T>>() {
			@Override
			public List<T> run(EntityManager em) {
				TypedQuery<T> queryObject = em.createQuery(query, type);
				if (params != null) {
					for (Map.Entry<String, Object> entry : params.entrySet()) {
						queryObject.setParameter(entry.getKey(), entry.getValue());
					}
				}
				return queryObject.getResultList();
			}
		});
	}

	public List<T> namedQuery(String queryName) {
		return namedQuery(queryName, new Object[0]);
	}

	public List<T> namedQuery(final String queryName, final Object... values) {
		return executeFind(new JpaAction<List<T>>() {
			@Override
			public List<T> run(EntityManager em) {
				TypedQuery<T> queryObject = em.createNamedQuery(queryName, type);
				for (int i = 0; i < values.length; i++) {
					queryObject.setParameter(i + 1, values[i]);
				}
				return queryObject.getResultList();
			}
		});
	}

	public List<T> namedQuery(final String queryName, final Map<String, Object> params) {
		return executeFind(new JpaAction<List<T>>() {
			@Override
			public List<T> run(EntityManager em) {
				TypedQuery<T> queryObject = em.createNamedQuery(queryName, type);
				if (params != null) {
					for (Map.Entry<String, Object> entry : params.entrySet()) {
						queryObject.setParameter(entry.getKey(), entry.getValue());
					}
				}
				return queryObject.getResultList();
			}
		});
	}
}
