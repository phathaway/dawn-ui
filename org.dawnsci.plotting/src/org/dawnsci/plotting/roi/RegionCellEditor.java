package org.dawnsci.plotting.roi;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.roi.IROI;

public class RegionCellEditor extends DialogCellEditor {

	private static final Logger logger = LoggerFactory.getLogger(RegionCellEditor.class);
	private IRegionTransformer transformer;
	
	public RegionCellEditor(Composite parent) {
		this(parent, null);
	}

	public RegionCellEditor(Composite parent, IRegionTransformer transformer) {
		super(parent);
		this.transformer = transformer;
	}
	
	@Override
	protected Object openDialogBox(Control cellEditorWindow) {
						
		final ROIDialog dialog = new ROIDialog(cellEditorWindow.getShell()); // extends BeanDialog
		dialog.create();
		dialog.getShell().setSize(550,450); // As needed
		dialog.getShell().setText("Edit Region of Interest");
	
		try {
			dialog.setROI(transformer!=null ? transformer.getROI() : (IROI)getValue());
	        final int ok = dialog.open();
	        if (ok == Dialog.OK) {
	            return transformer!=null ? transformer.getValue(dialog.getROI()) : dialog.getROI();
	        }
		} catch (Exception ne) {
			logger.error("Problem decoding and/or encoding bean!", ne);
		}
        
        return null;
	}
    protected void updateContents(Object value) {
        if ( getDefaultLabel() == null) {
			return;
		}
        getDefaultLabel().setText(transformer!=null ? transformer.getRendererText() : getValue().toString());
    }

};
