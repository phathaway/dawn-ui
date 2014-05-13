package org.dawnsci.plotting.tools.powderintegration;

import org.dawnsci.common.widgets.decorator.FloatDecorator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.mihalis.opal.checkBoxGroup.CheckBoxGroup;

public class PowderCorrectionWidget {

	PowderCorrectionModel model = new PowderCorrectionModel();
	
	public PowderCorrectionWidget(Composite composite) {
		
		composite.setLayout(new GridLayout());
		
		final CheckBoxGroup polarGroup = new CheckBoxGroup(composite, SWT.NONE);
		polarGroup.setText("Apply Polarisation Correction");
		Composite content = polarGroup.getContent();
		content.setLayout(new GridLayout(2,true));
		Label lbl = new Label(content, SWT.None);
		lbl.setText("Polarisation Factor:");
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER,true, false));
		
		final Text factoTxt = new Text(content, SWT.SINGLE | SWT.RIGHT);
		factoTxt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER,true, false));
		factoTxt.setText(String.valueOf(model.getPolarisationFactor()));
		final FloatDecorator fd = new FloatDecorator(factoTxt);
		fd.setMinimum(0);
		fd.setMaximum(1);
		
		factoTxt.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				if (!fd.isError()) {
					try {
						model.setPolarisationFactor(Double.parseDouble(factoTxt.getText()));
					} catch (NumberFormatException nfe) {
						//do nothing
					}
				}
			}
		});
		
		lbl = new Label(content, SWT.None);
		lbl.setText("Angular Offset:");
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER,true, false));
		
		final Text angleTxt = new Text(content, SWT.SINGLE | SWT.RIGHT);
		angleTxt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER,true, false));
		angleTxt.setText(String.valueOf(model.getPolarisationAngularOffset()));
		final FloatDecorator fd2 =new FloatDecorator(angleTxt);
		fd2.setMinimum(-180);
		fd2.setMaximum(180);
		
		angleTxt.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				if (!fd2.isError()) {
					try {
						model.setPolarisationAngularOffset(Double.parseDouble(angleTxt.getText()));
					} catch (NumberFormatException nfe) {
						//do nothing
					}
				}
			}
		});
		
		if (model.isApplyPolarisationCorrection()) polarGroup.activate();
		else polarGroup.deactivate(); 
		
		polarGroup.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				model.setApplyPolarisationCorrection(polarGroup.isActivated());
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		
		final Button solidAngleCorrection = new Button(composite, SWT.CHECK);
		solidAngleCorrection.setText("Apply Solid Angle Correction");
		
		solidAngleCorrection.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				model.setApplySolidAngleCorrection(solidAngleCorrection.getSelection());
				
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
	}
	
	public PowderCorrectionModel getModel() {
		return model;
	}
}