package fr.esrf.icat.manager.core.icatserver;

/*
 * #%L
 * icat-manager :: core
 * %%
 * Copyright (C) 2014 ESRF - The European Synchrotron
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.esrf.icat.client.ICATClientException;
import fr.esrf.icat.client.SimpleICATClient;
import fr.esrf.icat.client.wrapper.WrappedEntityBean;
import fr.esrf.icat.manager.core.ICATDataService;

public class EntityContentProvider implements  IStructuredContentProvider {

	private final static Logger LOG = LoggerFactory.getLogger(EntityContentProvider.class);
	private static final String DEFAULT_INCLUDE = "INCLUDE 1";
	private static final String ORDER_CLAUSE = "ORDER BY";
	private static final String ORDER_UP = "ASC";
	private static final String ORDER_DOWN = "DESC";
	private static final int DEFAULT_PAGE_SIZE = 50;
	
	private String sortingField;
	private int sortingOrder;
	private int offset;
	private int pageSize;
	private int currentPageSize;
	private ICATEntity entity;
	private Object[] elements;
	private String filterText;
	private boolean sortByName;
	private boolean keepCount;
	private long count;
	
	public EntityContentProvider() {
		super();
		this.elements = null; 
		this.sortingField = ICATEntity.ID_FIELD;
		this.sortingOrder = SWT.DOWN;
		this.offset = 0;
		this.pageSize = DEFAULT_PAGE_SIZE;
		this.currentPageSize = 0;
		this.filterText = null;
		this.sortByName = false;
		this.keepCount = false;
	}

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if(null == inputElement || !(inputElement instanceof ICATEntity)) {
			return null;
		}
		this.entity = (ICATEntity) inputElement;
		return null == elements ? new Object[0] : elements;
	}

	private String makeSearchString(final ICATEntity entity) {
		StringBuilder sb = new StringBuilder();
		sb.append(offset);
		sb.append(',');
		sb.append(pageSize);
		sb.append(' ');
		sb.append(entity.getEntityName());
		sb.append(' ');
		sb.append(DEFAULT_INCLUDE);
		if(sortingField != null) {
			sb.append(' ');
			sb.append(ORDER_CLAUSE);
			sb.append(' ');
			sb.append(sortingField);
			if(sortByName && ICATDataService.getInstance().canSortByName(entity, sortingField)) {
				sb.append('.');
				sb.append(ICATEntity.NAME_FIELD);
			}
			sb.append(' ');
			if(sortingOrder == SWT.UP) {
				sb.append(ORDER_UP);
			} else {
				sb.append(ORDER_DOWN);
			}
		}
		if(null != filterText && !filterText.isEmpty()) {
			sb.append(" [");
			sb.append(filterText);
			sb.append("]");
		}
		return sb.toString();
	}

	public void toggleSortingField(final String field) {
		if(field.equals(sortingField)) {
			if(sortingOrder == SWT.UP) {
				sortingOrder = SWT.DOWN;
			} else {
				sortingOrder = SWT.UP;
			}
		} else {
			sortingField = field;
			sortingOrder = SWT.DOWN;
		}
		offset = 0;
	}

	public void setFilterString(String text) {
		this.filterText = text;
		this.offset = 0;
	}

	public String getSortingField() {
		return sortingField;
	}
	
	public int getSortingOrder() {
		return sortingOrder;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public int getOffset() {
		return offset;
	}
	
	public void nextPage() {
		if(currentPageSize == pageSize) {
			offset += pageSize;
		}
	}
	
	public void previousPage() {
		offset = Math.max(0, offset - pageSize);
	}
	
	public void gotToFirst() {
		offset = 0;
	}

	public boolean isFirstPage() {
		return offset == 0;
	}
	
	public boolean isLastPage() {
		return currentPageSize < pageSize;
	}
	
	public String getPaginationLabelText() {
		StringBuilder sb = new StringBuilder();
		if(currentPageSize == 0) {
			if( offset == 0) {
				sb.append("No ");
			} else {
				sb.append("No more ");
			}
			if(null != filterText && !filterText.isEmpty()) {
				sb.append("filtered ");
			}
			sb.append(entity.getEntityName());
			sb.append(" to display");
			if(keepCount && offset > 0) {
				sb.append(" / ");
				sb.append(count);
			}
		} else {
			if(null != filterText && !filterText.isEmpty()) {
				sb.append("Filtered ");
			}
			sb.append(entity.getEntityName());
			sb.append(" from ");
			sb.append(offset + 1);
			sb.append(" to ");
			sb.append(offset + currentPageSize);
			if(keepCount) {
				sb.append(" / ");
				sb.append(count);
			}
		}
		return sb.toString();
	}

	public void fetch(final ICATEntity toFetch) throws ICATClientException {
		this.entity = toFetch;
		final String searchString = makeSearchString(entity);
		if(LOG.isDebugEnabled()) {
			LOG.debug("Getting content of " + entity.getEntityName() + " from " + entity.getServer().getServerURL());
			LOG.debug(searchString);
		}
		try {
			SimpleICATClient client = ICATDataService.getInstance().getClient(entity.getServer());
			List<WrappedEntityBean> search = client.search(searchString);
			currentPageSize = null == search ? 0 : search.size();
			elements = null == search ? null : search.toArray();
		} catch (ICATClientException e) {
			LOG.error("Unable to load entity content for entity " + entity.getEntityName(), e);
			elements = null;
			currentPageSize = 0;
			count = 0;
			throw e;
		}
		
		if(keepCount) {
			try {
				count = ICATDataService.getInstance().getEntityCount(entity, filterText);
			} catch (ICATClientException e) {
				LOG.error("Unable to load entity content for entity " + entity.getEntityName(), e);
				count = 0;
			}
		}

	}

	public void toggleNameSorting() {
		sortByName = !sortByName;
	}

	public void setNameSorting(final boolean sortByName) {
		this.sortByName = sortByName;
	}

	public void toggleKeepCount() {
		keepCount = !keepCount;
	}

	public void setKeepCount(final boolean keepCount) {
		this.keepCount = keepCount;
	}
}
