 
package fr.esrf.icat.manager.core.handlers;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.esrf.icat.manager.core.icatserver.ICATEntity;
import fr.esrf.icat.manager.core.part.DataPart;

public class OpenEntityHandler {
	
	private final static Logger LOG = LoggerFactory.getLogger(OpenEntityHandler.class);
	
	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SELECTION)@Optional ICATEntity entity, final EPartService partService,
			 final EModelService modelService, final MWindow window) {
		OpenEntityHandler.openEntityPart(partService, modelService, window, entity);
	}
	
	
	@CanExecute
	public boolean canExecute(@Named(IServiceConstants.ACTIVE_SELECTION)@Optional ICATEntity entity) {
		return entity != null; 
	}


	public static void openEntityPart(final EPartService partService, final EModelService modelService, final MWindow window, ICATEntity entity) {
		String partID = DataPart.DATA_PART_ELEMENT_HEADER + ":" + entity.getServer().getServerURL() + ":" + entity.getEntityName();
		MPart mPart = (MPart) modelService.find(partID, window);
		if(null != mPart) {
		    partService.showPart(mPart, PartState.ACTIVATE);
		    LOG.debug("Showing existing part: " + partID);
		    return;
		}
		mPart = partService.createPart(DataPart.DATA_PART_DESCRIPTOR);
		mPart.setElementId(partID);
		mPart.setLabel(entity.getEntityName() + " [" + entity.getServer().getServerURL() + "]");
		mPart.setObject(entity);
		MPartStack partstack = (MPartStack) modelService.find(DataPart.ICAT_MANAGER_MAINSTACK, window);
		partstack.getChildren().add(mPart);
		partService.showPart(mPart, PartState.ACTIVATE);
		LOG.debug("Creating new part " + partID);
	}
		
}