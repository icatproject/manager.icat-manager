package fr.esrf.icat.manager.core.part;

import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import net.sf.swtaddons.autocomplete.combo.AutocompleteComboSelector;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.fieldassist.ComboContentAdapter;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposalListener2;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.nebula.widgets.cdatetime.CDT;
import org.eclipse.nebula.widgets.cdatetime.CDateTime;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.esrf.icat.client.SimpleICATClient;
import fr.esrf.icat.client.wrapper.WrappedEntityBean;
import fr.esrf.icat.manager.core.icatserver.EntityListProposalContentProvider;
import fr.esrf.icat.manager.core.icatserver.ICATEntity;

public class EntityEditDialog extends Dialog {

	private final static Logger LOG = LoggerFactory.getLogger(EntityEditDialog.class);
	
	private final static char[] DEFAULT_ACTIVATION_CHARS = ("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_12345679890" + SWT.BS + SWT.DEL).toCharArray();
	private final static KeyStroke DEFAULT_KEYSTROKE = KeyStroke.getInstance(SWT.CTRL, ' ');
	private final static Image WARNING_IMAGE;

	static {
	    Bundle bundle = FrameworkUtil.getBundle(EntityEditDialog.class);
	    URL url = FileLocator.find(bundle, new Path("icons/warning.gif"), null);
	    WARNING_IMAGE = ImageDescriptor.createFromURL(url).createImage();
	}
	
	private WrappedEntityBean entity;
	private SimpleICATClient client;
	private Map<String, Pair<Object[], Combo>> comboMapping;
	
	public EntityEditDialog(final Shell parentShell, final WrappedEntityBean entity, final SimpleICATClient client) {
		super(parentShell);
		this.entity = entity;
		this.client = client;
	}

	public WrappedEntityBean getEntity() {
		return entity;
	}

	@Override
	protected Control createDialogArea(final Composite parent) {
		final Composite container = (Composite) super.createDialogArea(parent);
	    GridLayout layout = new GridLayout(2, false);
	    layout.marginRight = 5;
	    layout.marginLeft = 10;
	    container.setLayout(layout);
	    comboMapping = new HashMap<>();
	    
	    for(final String field : entity.getMutableFields()) {
		    Label lblAuthn = new Label(container, SWT.NONE);
		    lblAuthn.setText(StringUtils.capitalize(field) + ":");
		    final Class<?> clazz = entity.getReturnType(field);
		    Object initialValue = null;
		    try {
				initialValue = entity.get(field);
			} catch (Exception e) {
				LOG.error("Error getting initial value for " + field, e);
			}
		    final boolean hasInitialValue = initialValue != null;
			if(entity.isEntity(field)) {
				final Combo combo = new Combo(container, SWT.DROP_DOWN | SWT.BORDER);
				final Label label = new Label(container, SWT.RIGHT);
				label.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));
				label.setImage(WARNING_IMAGE);
				final Label warningLabel = new Label(container, SWT.LEFT);
				warningLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false));
				warningLabel.setText(EntityListProposalContentProvider.INITIAL_FILTER);
				combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
				final EntityListProposalContentProvider proposalProvider = new EntityListProposalContentProvider(
						client, entity.getReturnType(field).getSimpleName(), (WrappedEntityBean) initialValue);
				final ContentProposalAdapter contentProposalAdapter = new ContentProposalAdapter(
						combo, new ComboContentAdapter(), proposalProvider, DEFAULT_KEYSTROKE, DEFAULT_ACTIVATION_CHARS);
				contentProposalAdapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
				contentProposalAdapter.setPropagateKeys(true);
				contentProposalAdapter.setAutoActivationDelay(800);
				contentProposalAdapter.addContentProposalListener(new IContentProposalListener2() {
					@Override
					public void proposalPopupOpened(ContentProposalAdapter adapter) {
					}
					@Override
					public void proposalPopupClosed(ContentProposalAdapter adapter) {
						// when the proposal popup closes we update the helper label
						warningLabel.setText(proposalProvider.getCurrentFilter());
						// when the proposal popup closes we set the content of the combo to the proposals
						combo.setItems(proposalProvider.getCurrentItems());
						// and select the proper item
						if(hasInitialValue) {
							combo.select(0);
						} else {
							combo.deselectAll();
						}
						// needed for proper label display
						container.layout();
					}
				});
				combo.setItems(proposalProvider.getInitialItems());
				if(hasInitialValue) {
					combo.select(0);
				}
				comboMapping.put(field, new ImmutablePair<Object[], Combo>(new Object[]{proposalProvider}, combo));
				
			} else if(Enum.class.isAssignableFrom(clazz)){
				final Combo combo = new Combo(container, SWT.DROP_DOWN | SWT.BORDER);
				final Object[] c = clazz.getEnumConstants();
				final String[] s = new String[c.length];
				int selected = -1;
				for(int i = 0; i < c.length; i++) {
					s[i] = c[i].toString();
					if(initialValue != null && c[i].equals(initialValue)) {
						selected = i;
					}
				}
				new AutocompleteComboSelector(combo);
				combo.setItems(s);
				if(selected >= 0) {
					combo.select(selected);
				}
				combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
				comboMapping.put(field, new ImmutablePair<Object[], Combo>(c, combo));
				
			} else if(clazz.equals(Boolean.class) || clazz.equals(boolean.class)) {
				final Button btn = new  Button(container, SWT.CHECK);
				btn.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
				if(null == initialValue) {
					try {
						entity.set(field, Boolean.FALSE);
					} catch (Exception e) {
						LOG.error("Error setting " + field + " to " + Boolean.FALSE);
					}
				} else {
					btn.setSelection((Boolean) initialValue);
				}
				btn.addSelectionListener(new SelectionAdapter(){
					@Override
					public void widgetSelected(SelectionEvent e) {
						boolean value = btn.getSelection();
						try {
							entity.set(field, value);
						} catch (Exception e1) {
							LOG.error("Error setting " + field + " to " + value, e1);
						}
					}
				});
				
			} else if(Calendar.class.isAssignableFrom(clazz)
					|| Date.class.isAssignableFrom(clazz) ||
					XMLGregorianCalendar.class.isAssignableFrom(clazz)) {
				
				final CDateTime cdt = new CDateTime(container,  CDT.BORDER | CDT.SPINNER | CDT.TAB_FIELDS | CDT.DATE_MEDIUM | CDT.TIME_MEDIUM);
				cdt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
				if(null != initialValue) {
					Date initialDate = null;
					if(initialValue instanceof Calendar) {
						initialDate = ((Calendar)initialValue).getTime();
					} else if(initialValue instanceof XMLGregorianCalendar) {
						initialDate = ((XMLGregorianCalendar)initialValue).toGregorianCalendar().getTime();
					} else if(initialValue instanceof Date) {
						initialDate = (Date) initialValue;
					}
					cdt.setSelection(initialDate);
				}

				cdt.addSelectionListener(new SelectionAdapter(){
					@Override
					public void widgetSelected(SelectionEvent e) {
						Date value = cdt.getSelection();
						Object o = value;
						if(Calendar.class.isAssignableFrom(clazz)) {
							Calendar c = GregorianCalendar.getInstance();
							c.setTime((Date)value);
							o = c;
						} else if(clazz.equals(XMLGregorianCalendar.class)) {
							GregorianCalendar c = (GregorianCalendar) GregorianCalendar.getInstance();
							c.setTime((Date)value);
							try {
								o = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
							} catch (DatatypeConfigurationException e1) {
								LOG.error("Unable to create XMLGregorianCalendar", e1);
								return;
							}
						}
						try {
							entity.set(field, o);
						} catch (Exception e1) {
							LOG.error("Error setting " + field + " to " + o, e1);
						}
					}
				});
				
			} else if(Number.class.isAssignableFrom(clazz)){
				final Text text = new Text(container, SWT.BORDER);
				text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
				if(null != initialValue) {
					text.setText(initialValue.toString());
				}
				final Color original = text.getForeground();
				text.addModifyListener(new ModifyListener(){
					@Override
					public void modifyText(ModifyEvent e) {
						text.setForeground(original);
						String value = text.getText();
						if(null == value) {
							value = ICATEntity.EMPTY_STRING;
						}
						try {
							entity.set(field, clazz.getMethod("valueOf", new Class<?>[]{String.class}).invoke(null, value));
						} catch (Exception e1) {
							LOG.error("Error setting " + field + " to " + value, e1);
							text.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
						}
					}
				});
				
			} else { // Assumes String
				final Text text = new Text(container, SWT.BORDER);
				text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
				if(null == initialValue) {
					try {
						entity.set(field, ICATEntity.EMPTY_STRING);
					} catch (Exception e) {
						LOG.error("Error setting " + field + " to EMPTY_STRING");
					}
				} else {
					text.setText(initialValue.toString());
				}
				text.addModifyListener(new ModifyListener(){
					@Override
					public void modifyText(ModifyEvent e) {
						String value = text.getText();
						if(null == value) {
							value = ICATEntity.EMPTY_STRING;
						}
						try {
							entity.set(field, value);
						} catch (Exception e1) {
							LOG.error("Error setting " + field + " to " + value, e1);
						}
					}
				});
			}
		    
	    }
		return container;
	}

	@Override
	protected void cancelPressed() {
		comboMapping = null;
		super.cancelPressed();
	}

	@Override
	protected void okPressed() {
		for(Entry<String, Pair<Object[], Combo>> entry : comboMapping.entrySet()) {
			final String field = entry.getKey();
			final Pair<Object[], Combo> pair = entry.getValue();
			final Combo combo = pair.getRight();
			final Object[] left = pair.getLeft();
			final Object[] objects = left[0] instanceof EntityListProposalContentProvider ?
					((EntityListProposalContentProvider)left[0]).getCurrentObjects(): left;
			final Object value = objects[combo.indexOf(combo.getText())];
			try {
				entity.set(field, value);
			} catch (Exception e) {
				LOG.error("Error setting " + field + " to " + value, e);
			}
		}
		comboMapping = null;
		super.okPressed();
	}

}
