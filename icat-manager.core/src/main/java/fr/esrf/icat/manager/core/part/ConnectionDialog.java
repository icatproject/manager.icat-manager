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


import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.layout.GridData;

public class ConnectionDialog extends Dialog {

	private Text txtAuthn;
	private Text txtUser;
	private Text txtPassword;
	private String authn = "";
	private String user = "";
	private String password = "";

	public ConnectionDialog(Shell parentShell, String authn, String user) {
		super(parentShell);
		this.authn = authn;
		this.user = user;
	}

	  @Override
	  protected Control createDialogArea(Composite parent) {
	    Composite container = (Composite) super.createDialogArea(parent);
	    GridLayout layout = new GridLayout(2, false);
	    layout.marginRight = 5;
	    layout.marginLeft = 10;
	    container.setLayout(layout);

	    Label lblAuthn = new Label(container, SWT.NONE);
	    lblAuthn.setText("Authentication:");

	    txtAuthn = new Text(container, SWT.BORDER);
	    txtAuthn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
	    txtAuthn.setText(authn);

	    Label lblUser = new Label(container, SWT.NONE);
	    lblUser.setText("User:");

	    txtUser = new Text(container, SWT.BORDER);
	    txtUser.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
	    txtUser.setText(user);

	    Label lblPassword = new Label(container, SWT.NONE);
	    GridData gd_lblNewLabel = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
	    gd_lblNewLabel.horizontalIndent = 1;
	    lblPassword.setLayoutData(gd_lblNewLabel);
	    lblPassword.setText("Password:");

	    txtPassword = new Text(container, SWT.BORDER);
	    txtPassword.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
	    txtPassword.setEchoChar('*');
	    
	    if(null == authn || authn.isEmpty()) {
	    	txtAuthn.setFocus();
	    } else {
	    	txtPassword.setFocus();
	    }
	    return container;
	  }

	  // override method to use "Login" as label for the OK button
	  @Override
	  protected void createButtonsForButtonBar(Composite parent) {
	    createButton(parent, IDialogConstants.OK_ID, "Login", true);
	    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	  }

	  @Override
	  protected void okPressed() {
	    // Copy data from SWT widgets into fields on button press.
	    // Reading data from the widgets later will cause an SWT
	    // widget disposed exception.
	    user = txtUser.getText();
	    password = txtPassword.getText();
	    authn = txtAuthn.getText();
	    super.okPressed();
	  }

	  public String getUser() {
	    return user;
	  }

	  public void setUser(String user) {
	    this.user = user;
	  }

	  public String getPassword() {
	    return password;
	  }

	  public void setPassword(String password) {
	    this.password = password;
	  }

	public String getAuthenticationMethod() {
		return authn;
	}

	public void setAuthenticationMethod(String authn) {
		this.authn = authn;
	}

}
