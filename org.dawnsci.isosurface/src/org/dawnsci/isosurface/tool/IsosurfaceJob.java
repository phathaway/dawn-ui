/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.isosurface.tool;

import java.util.ArrayList;
import java.util.List;

import org.dawb.common.ui.monitor.ProgressMonitorWrapper;
import org.dawnsci.isosurface.alg.MarchingCubesModel;
import org.dawnsci.isosurface.alg.Surface;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.dawnsci.analysis.api.dataset.IDataset;
import org.eclipse.dawnsci.analysis.api.dataset.ILazyDataset;
import org.eclipse.dawnsci.analysis.api.processing.IOperation;
import org.eclipse.dawnsci.analysis.dataset.impl.FloatDataset;
import org.eclipse.dawnsci.analysis.dataset.impl.IntegerDataset;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.dawnsci.plotting.api.trace.IIsosurfaceTrace;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.nebula.visualization.xygraph.linearscale.Tick;
import org.eclipse.nebula.visualization.xygraph.linearscale.TickFactory;
import org.eclipse.nebula.visualization.xygraph.linearscale.TickFactory.TickFormatting;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
 * @author nnb55016 / Joel Ogden
 * The Job class for Isovalue visualisation feature
 */
public class IsosurfaceJob extends Job {

	private static final Logger logger = LoggerFactory.getLogger(IsosurfaceJob.class);
	
 	private IOperation<MarchingCubesModel, Surface> generator;
 	final private IPlottingSystem system;
 	private String name;
 	
 	private Double value;
	private double opacity;
	private int[] boxSize;
	private RGB colour;
 	private String traceName;
 	
 	private ILazyDataset slice;
 	
	public IsosurfaceJob(String name, IPlottingSystem system,  ILazyDataset slice, IOperation<MarchingCubesModel, Surface> generator)
	{
		super(name);
		
		setUser(false);
		setPriority(Job.INTERACTIVE);
		
		this.name = name;
		this.system = system;
		this.slice = slice;
		this.generator = generator;
		
	}
	
	/**
	 * Call to calculate and draw the isosurface
	 * 
	 * @param boxSize - representing XYZ sizes, Int[3] array
	 * @param value - The value to be rendered
	 * @param opacity - The transparency
	 * @param colour - The colour of the surface
	 * @param traceName - The name of the surface trace
	 * 
	 */
	
	public void compute(
			int[] boxSize, 
			Double value,  
			double opacity, 
			RGB colour, 
			String traceName,
			String beanName)
	{		
		this.boxSize = boxSize;   
		this.value = value;     
		this.opacity = opacity;   
		this.colour = colour;
		this.traceName = traceName;
		this.name = beanName;
		
		cancel();
		schedule();		
	}
	
	// made to seperate computing the isosurface and simply updating the values like colour
	public void update (
			int[] boxSize, 
			Double value,  
			double opacity, 
			RGB colour, 
			String traceName, 
			String beanName)
	{		
		if ((IIsosurfaceTrace) system.getTrace(traceName) != null && system.getTrace(traceName).getData() != null)
		{
			compute(null, null, opacity, colour, traceName, beanName);
		}
		else
		{
			compute(boxSize, value, opacity, colour, traceName, beanName);
		}
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor)
	{
		Thread.currentThread().setName("IsoSurface - " + name);
		final IIsosurfaceTrace trace;
		boolean createTrace = false; // this is going to need some reorganising
		
		// create the trace if required, if not get the trace
		if ((IIsosurfaceTrace) system.getTrace(traceName) == null)
		{
			trace = system.createIsosurfaceTrace(this.traceName);
			trace.setName(this.traceName);
			createTrace = true;
		}
		else
		{
			trace = (IIsosurfaceTrace) system.getTrace(traceName);
		}
		
		MarchingCubesModel model = this.generator.getModel();
		
		model.setBoxSize(boxSize);
		model.setOpacity(opacity);
		model.setIsovalue(value);
		model.setColour(colour.red, colour.green, colour.blue);
				
		try 
		{
			system.setDefaultCursor(IPlottingSystem.WAIT_CURSOR);
			
			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;
			
			try
			{
				if (generator.getModel().getLazyData() != slice)
				{
					generator.getModel().setLazyData(slice);
				}
				
				IDataset points     = null;
				IDataset textCoords = null;
				IDataset faces      = null;
				
				if (value != null || trace.getData() == null)
				{
					Surface surface = generator.execute(null, new ProgressMonitorWrapper(monitor));
					
					points     = new FloatDataset(surface.getPoints(), surface.getPoints().length);
					textCoords = new FloatDataset(surface.getTexCoords(), surface.getTexCoords().length);
					faces      = new IntegerDataset(surface.getFaces(), surface.getFaces().length);
				}
												
				final ArrayList<IDataset> axis = new ArrayList<IDataset>();
				
				TickFactory tickGenerator = new TickFactory(TickFormatting.autoMode, null);
				
				
				// set the data set size
				axis.add(new IntegerDataset(this.slice.getShape(), null));
				
				axis.add(convertTodatasetAxis(tickGenerator.generateTicks(0, slice.getShape()[0], 15, false, false)));
				axis.add(convertTodatasetAxis(tickGenerator.generateTicks(0, slice.getShape()[1], 15, false, false)));
				axis.add(convertTodatasetAxis(tickGenerator.generateTicks(0, slice.getShape()[2], 15, false, false)));
								
				final int[] traceColour = new int[]{colour.red, colour.green, colour.blue};
				final double traceOpacity = opacity;
				
				trace.setMaterial(traceColour[0], traceColour[1] , traceColour[2], traceOpacity);
				trace.setData(points, textCoords, faces, axis );
			
				if (createTrace)
				{
					Display.getDefault().syncExec(new Runnable() {
						public void run() {
							system.addTrace(trace);
				    	}
				    });
				}
				
			} 
			catch (UnsupportedOperationException e)
			{
				e.printStackTrace();
				showErrorMessage("The number of vertices has exceeded "+ generator.getModel().getVertexLimit(), "The surface cannot be rendered. Please increase the box size.");
				return Status.CANCEL_STATUS;
				
			} 
			catch (Exception e) 
			{
				logger.error("Cannot run algorithm "+ generator.getClass().getSimpleName(), e);
				return Status.CANCEL_STATUS;
				
			} 
			catch (OutOfMemoryError e)
			{
				e.printStackTrace();
				showErrorMessage("Out of memory Error", "There is not enough memory to render the surface. Please increase the box size.");
				return Status.CANCEL_STATUS;
			}
			
		}
		finally
		{
            monitor.done();
			system.setDefaultCursor(IPlottingSystem.NORMAL_CURSOR);
		}
		return Status.OK_STATUS;
	}
	
	
	private FloatDataset convertTodatasetAxis(List<Tick> tickList) {
		
				
		float[] ticks = new float[tickList.size()];
		
		int i = 0;
		for (Tick t: tickList)
		{
			ticks[i] = (float) t.getValue();
			i++;
		}		
		
		return new FloatDataset(ticks, null);
	}

	/*
	 * look into improving !!
	 */
	private ArrayList<IDataset> generateDuplicateAxes(int count, int step)
	{
		ArrayList<IDataset> axis = new ArrayList<IDataset>();
		
		float[] axisArray = new float[10];
		for (int i = 0; i < count; i ++)
		{
			axisArray[i] = i*step;
		}
		
		axis.add(new FloatDataset(new float[]{
										slice.getShape()[0],
										slice.getShape()[1],
										slice.getShape()[2]}));
		axis.add(new FloatDataset(axisArray , null));
		axis.add(new FloatDataset(axisArray , null));
		axis.add(new FloatDataset(axisArray , null));
	
		return axis;
	}
	
	private void showErrorMessage(final String title, final String message) {
		Display.getDefault().syncExec(new Runnable(){
			@Override
			public void run() {
				MessageDialog.openError(Display.getDefault().getActiveShell(), title, message);
			}
		});
	}
		
	public void destroy(String traceName)
	{
		if (system.getTrace(traceName) != null)
		{ 
			system.getTrace(traceName).dispose();
		}
	}
	
	public IOperation<MarchingCubesModel, Surface> getGenerator()
	{
		return this.generator;
	}

}
