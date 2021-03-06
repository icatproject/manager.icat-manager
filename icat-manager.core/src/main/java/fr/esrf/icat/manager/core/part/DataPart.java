package fr.esrf.icat.manager.core.part;

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


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.e4.ui.workbench.swt.modeling.EMenuService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.esrf.icat.client.ICATClientException;
import fr.esrf.icat.client.wrapper.WrappedEntityBean;
import fr.esrf.icat.manager.core.ICATDataService;
import fr.esrf.icat.manager.core.icatserver.DataColumnEditingSupport;
import fr.esrf.icat.manager.core.icatserver.DataColumnLabelProvider;
import fr.esrf.icat.manager.core.icatserver.EntityContentProvider;
import fr.esrf.icat.manager.core.icatserver.ICATEntity;

@SuppressWarnings("restriction")
public class DataPart {

	public static final String ICAT_MANAGER_MAINSTACK = "icat-manager.core.partstack.mainstack";
	public static final String DATA_PART_ELEMENT_HEADER = "icat-manager.core.part.data";
	public static final String DATA_PART_DESCRIPTOR = "icat-manager.core.partdescriptor.datapart";
	public static final String IN_PLACE_EDITING_PROPERTY_KEY = "inPlaceEditing";
	
	private final static Logger LOG = LoggerFactory.getLogger(DataPart.class);
	
	private final static File configfile = new File(System.getProperty("user.home"), ".icat_column_orders");

	private final static Properties columnProps;
	
	private final static Image IMAGE_UP;
	private final static Image IMAGE_DOWN;
	private final static Image IMAGE_NEXT;
	private final static Image IMAGE_PREVIOUS;
	private final static Image IMAGE_FIRST;
	private final static Image IMAGE_CLEAR;
	
	static {
	    Bundle bundle = FrameworkUtil.getBundle(DataPart.class);
	    URL url = FileLocator.find(bundle, new Path("icons/arrow_up.png"), null);
	    IMAGE_UP = ImageDescriptor.createFromURL(url).createImage();
	    url = FileLocator.find(bundle, new Path("icons/arrow_down.png"), null);
	    IMAGE_DOWN = ImageDescriptor.createFromURL(url).createImage();
	    url = FileLocator.find(bundle, new Path("icons/next.gif"), null);
	    IMAGE_NEXT = ImageDescriptor.createFromURL(url).createImage();
	    url = FileLocator.find(bundle, new Path("icons/prev.gif"), null);
	    IMAGE_PREVIOUS = ImageDescriptor.createFromURL(url).createImage();
	    url = FileLocator.find(bundle, new Path("icons/first.png"), null);
	    IMAGE_FIRST = ImageDescriptor.createFromURL(url).createImage();
	    url = FileLocator.find(bundle, new Path("icons/clear_co.gif"), null);
	    IMAGE_CLEAR = ImageDescriptor.createFromURL(url).createImage();
	    columnProps = new Properties();
		LOG.debug("Reading column order file at: " + configfile.getAbsolutePath());
		if(configfile.exists()) {
			FileInputStream inStream = null;
			try {
				inStream = new FileInputStream(configfile);
				columnProps.load(inStream);
			} catch(IOException e) {
				LOG.error("Error reading column file " + configfile.getAbsolutePath(), e);
			} finally {
				if(inStream != null) {
					try {
						inStream.close();
					} catch (Exception e) {
						LOG.warn("Error closing file " + configfile.getAbsolutePath(), e);
					}
				}
			}
		}

	}
	
	private TableViewer viewer;
	private ICATEntity entity;
	private Label paginationLabel;
	private EntityContentProvider contentProvider;
	private Button previousBtn;
	private Button nextBtn;
	private Button firstBtn;
	private Text filterText;
	private Button clearFilter;
	private ISelectionChangedListener selectionListener;

	@PostConstruct
	public void postConstruct(final Composite parent, final EMenuService menuService, 
			final MPart part, final ESelectionService selectionService) {
		
		final GridLayout pageLayout = new GridLayout();
		pageLayout.marginWidth = 0;
		pageLayout.marginHeight = 0;
		pageLayout.verticalSpacing = 0;
		parent.setLayout(pageLayout);
		
		Composite top = new Composite(parent, SWT.NONE);
		top.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, true, false));
		final RowLayout barLayout = new RowLayout();
		barLayout.center = true;
		barLayout.pack = true;
		barLayout.spacing = 5;
		top.setLayout(barLayout);
		
		paginationLabel = new Label(top, SWT.NONE);

		firstBtn = new Button(top, SWT.PUSH);
		firstBtn.setText("First");
		firstBtn.setImage(IMAGE_FIRST);
		
		previousBtn = new Button(top, SWT.PUSH);
		previousBtn.setText("Previous");
		previousBtn.setImage(IMAGE_PREVIOUS);
		
		nextBtn = new Button(top, SWT.PUSH);
		nextBtn.setText("Next");
		nextBtn.setImage(IMAGE_NEXT);
		
		new Label(top, SWT.NONE).setText("Page size:");
		final CCombo pageSizeCombo = new CCombo(top, SWT.DROP_DOWN  | SWT.BORDER);
		pageSizeCombo.setLayoutData(new RowData(SWT.DEFAULT, SWT.DEFAULT));
		pageSizeCombo.setItems(new String[]{"50", "100", "150", "200"});
		pageSizeCombo.select(0);
		pageSizeCombo.addVerifyListener(new VerifyListener() {
			@Override
			public void verifyText(VerifyEvent e) {
				e.doit = e.character == SWT.BS || e.character == SWT.DEL || e.text.matches("\\d+");
			}
		});
		new Label(top, SWT.NONE).setText("Filter:");
		filterText = new Text(top, SWT.BORDER);
		filterText.setLayoutData(new RowData(400, SWT.DEFAULT));
		filterText.addSelectionListener(new SelectionListener(){
			@Override
			public void widgetSelected(SelectionEvent e) {
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				contentProvider.setFilterString(filterText.getText());
				refresh();
			}
		});
		
		clearFilter = new Button(top, SWT.PUSH);
		clearFilter.setImage(IMAGE_CLEAR);
		clearFilter.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				filterText.setText("");
				contentProvider.setFilterString(null);
				refresh();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		contentProvider = new EntityContentProvider();
		viewer.setContentProvider(contentProvider);
		entity =(ICATEntity) part.getTransientData().get(ICATEntity.ENTITY_CONTEXT_KEY);
		final String initialFilter = (String) part.getTransientData().get(ICATEntity.ENTITY_FILTER_KEY);
		if(null != initialFilter) {
			contentProvider.setFilterString(initialFilter);
			filterText.setText(initialFilter);
		}

		pageSizeCombo.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				contentProvider.setPageSize(Integer.parseInt(pageSizeCombo.getText()));
				refresh();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				String text = pageSizeCombo.getText();
				if(null == text || text.isEmpty()) {
					pageSizeCombo.select(0);
					text = pageSizeCombo.getText();
				}
				contentProvider.setPageSize(Integer.parseInt(text));
				refresh();
			}
		});
		firstBtn.addSelectionListener(new SelectionListener(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				contentProvider.gotToFirst();
				refresh();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		previousBtn.addSelectionListener(new SelectionListener(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				contentProvider.previousPage();
				refresh();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		nextBtn.addSelectionListener(new SelectionListener(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				contentProvider.nextPage();
				refresh();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		viewer.setData(IN_PLACE_EDITING_PROPERTY_KEY, Boolean.FALSE);
		createColumns(parent);
		final Table table = viewer.getTable();
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table.setHeaderVisible(true);
		table.setLinesVisible(true); 
		viewer.setInput(entity);
		paginationLabel.setText(contentProvider.getPaginationLabelText());
		nextBtn.setEnabled(!contentProvider.isLastPage());
		previousBtn.setEnabled(!contentProvider.isFirstPage());
		// needed to have the selection in the service ...
	    selectionListener = new ISelectionChangedListener() {
	    	  @Override
	    	  public void selectionChanged(SelectionChangedEvent event) {
	    	    IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
	    	    selectionService.setSelection(selection.toList());
	    	  }
    	};
		viewer.addSelectionChangedListener(selectionListener); 
	    // context menu
	    menuService.registerContextMenu(viewer.getControl(), "icat-manager.core.popupmenu.entity");
	    // reset column order if saved
	    try {
		    final String pState = columnProps.getProperty(getPersistenceID());
		    if(pState != null) {
		    	String[] idxS = pState.split(",");
		    	if(idxS.length == viewer.getTable().getColumnCount()) {
			    	int[] indices = new int[idxS.length];
			    	for(int i = 0; i < indices.length; i++) {
			    		indices[i] = Integer.parseInt(idxS[i]);
			    	}
				    viewer.getTable().setColumnOrder(indices);
		    	} else {
		    		LOG.warn("DataPart viewer has {} columns while the saved state has {}, removing configuration",
		    				viewer.getTable().getColumnCount(), idxS.length);
		    		columnProps.remove(getPersistenceID());
		    	}
		    }
	    } catch (Exception e) {
	    	LOG.error("Error while setting persisted column back", e);
	    }
	    // final show
		table.pack();
		refresh();
	}

	private void createColumns(final Composite parent) {
		WrappedEntityBean data = null;
		try {
			data = ICATDataService.getInstance().getClient(entity.getServer()).create(entity.getEntityName());
		} catch (ICATClientException e) {
			LOG.error("Error creating " + entity.getEntityName(), e);
			return;
		}
		List<String> fields = new LinkedList<>();
		fields.addAll(data.getMutableFields());
//		fields.addAll(data.getAssociationFields());
		fields.addAll(data.getImmutableFields());
		
		for(final String field : fields) {
			TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
			final TableColumn currentColumn = column.getColumn();
			currentColumn.setText(field);
			currentColumn.setWidth(100);
			currentColumn.setResizable(true);
			currentColumn.setMoveable(true);
			if(field.equals(contentProvider.getSortingField())) {
				switch(contentProvider.getSortingOrder()) {
					case SWT.UP:   currentColumn.setImage(IMAGE_UP); break;
					case SWT.DOWN: currentColumn.setImage(IMAGE_DOWN); break;
					default: currentColumn.setImage(null); break;
				}
			} else {
				currentColumn.setImage(null); 
			}
		    
		    column.getColumn().addSelectionListener(new SelectionListener(){
				@Override
				public void widgetSelected(SelectionEvent e) {
					final TableColumn col = (TableColumn) e.getSource();
					final String sortingField = col.getText();
					DataPart.LOG.debug("Selected column " + sortingField);
					contentProvider.toggleSortingField(field);
					for(TableColumn column : viewer.getTable().getColumns()) {
						if(column.getText().equals(sortingField)) {
							switch(contentProvider.getSortingOrder()) {
								case SWT.UP:   column.setImage(IMAGE_UP); break;
								case SWT.DOWN: column.setImage(IMAGE_DOWN); break;
								default: column.setImage(null); break;
							}
						} else {
							column.setImage(null);
						}
					}
					refresh();
				}
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
				}
		    	
		    });

			column.setLabelProvider(new DataColumnLabelProvider(field));
			if(data.isMutable(field)) {
				column.setEditingSupport(new DataColumnEditingSupport(viewer, field, data.getReturnType(field), entity.getServer()));
			}
		}
	}
	
	public ICATEntity getEntity() {
		return entity;
	}

	public void refresh() {
		BusyIndicator.showWhile(null, new Runnable(){
			@Override
			public void run() {
				try {
					contentProvider.fetch(entity);
				} catch (ICATClientException e) {
					MessageDialog.openError(null, "Error", "Error feetching data:\n" + e.getMessage());
				}
				Display.getDefault().asyncExec(new Runnable(){
					@Override
					public void run() {
						viewer.refresh();
						refreshSelection();
						paginationLabel.setText(contentProvider.getPaginationLabelText());
						firstBtn.setEnabled(!contentProvider.isFirstPage());
						previousBtn.setEnabled(!contentProvider.isFirstPage());
						nextBtn.setEnabled(!contentProvider.isLastPage());
						clearFilter.setEnabled(!filterText.getText().isEmpty());
						// reset to 1st row
						viewer.getTable().setTopIndex(0);
						// this has to be called to avoid the label not resizing properly
						paginationLabel.getParent().getParent().layout();
					}
				});
			}
		});
	}
	
	public void toggleInPLaceEditing() {
		final Object value = viewer.getData(IN_PLACE_EDITING_PROPERTY_KEY);
		if(Boolean.FALSE.equals(value)) {
			viewer.setData(IN_PLACE_EDITING_PROPERTY_KEY, Boolean.TRUE);
		} else {
			viewer.setData(IN_PLACE_EDITING_PROPERTY_KEY, Boolean.FALSE);
		}
	}

	public void toggleNameSorting() {
		contentProvider.toggleNameSorting();
		refresh();
	}
	
	public void toggleKeepCount() {
		contentProvider.toggleKeepCount();
		refresh();
	}

	public void refreshSelection() {
		// needed to refresh potentially modified objects in the selection
		selectionListener.selectionChanged(null);
	}

	public void updateFilter(final String newFilter) {
		contentProvider.setFilterString(newFilter);
		filterText.setText(newFilter);
		refresh();
	}
	
	@PersistState
	public void saveColumnOrder(final MPart mPart) {
		columnProps.put(getPersistenceID(), StringUtils.join(viewer.getTable().getColumnOrder(), ','));
//		updateConfig();
	}

	private String getPersistenceID() {
		return entity.getServer().getServerURL() + ":" + entity.getEntityName();
	}
	
	public static void updateConfig(){
		LOG.debug("Writing column order file at: " + configfile.getAbsolutePath());
		FileOutputStream outStream = null;
		try {
			outStream = new FileOutputStream(configfile);
			columnProps.store(outStream, null);
		} catch(IOException e) {
			LOG.error("Error writing column file " + configfile.getAbsolutePath(), e);
		} finally {
			if(outStream != null) {
				try {
					outStream.close();
				} catch (Exception e) {
					LOG.warn("Error closing file " + configfile.getAbsolutePath(), e);
				}
			}
		}
	}
}
