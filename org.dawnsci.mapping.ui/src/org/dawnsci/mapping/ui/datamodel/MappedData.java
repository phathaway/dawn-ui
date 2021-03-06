package org.dawnsci.mapping.ui.datamodel;

import org.dawnsci.mapping.ui.MappingUtils;
import org.eclipse.dawnsci.analysis.api.dataset.IDataset;
import org.eclipse.dawnsci.analysis.api.dataset.ILazyDataset;
import org.eclipse.dawnsci.analysis.dataset.impl.Maths;
import org.eclipse.dawnsci.plotting.api.trace.MetadataPlotUtils;

public class MappedData extends AbstractMapData{

	
	public MappedData(String name, IDataset map, MappedDataBlock parent, String path) {
		super(name, map, parent, path);
	}
	
	public MappedData(String name, ILazyDataset map, MappedDataBlock parent, String path) {
		super(name,map, parent, path);
	}
	
	protected double[] calculateRange(ILazyDataset map){
		
		double[] range = MappingUtils.getGlobalRange(map);
		
		return range;
	}
	
	private int[] getIndices(double x, double y) {
		
		IDataset[] ax = MetadataPlotUtils.getAxesFromMetadata(map);
		
		IDataset xx = ax[1];
		IDataset yy = ax[0];
		
		double xMin = xx.min().doubleValue();
		double xMax = xx.max().doubleValue();
		
		double yMin = yy.min().doubleValue();
		double yMax = yy.max().doubleValue();
		
		double xd = ((xMax-xMin)/xx.getSize())/2;
		double yd = ((yMax-yMin)/yy.getSize())/2;
		
		if (x > xMax+xd || x < xMin-xd || y > yMax+yd || y < yMin-yd) return null;
		
		int xi = Maths.abs(Maths.subtract(xx, x)).argMin();
		int yi = Maths.abs(Maths.subtract(yy, y)).argMin();
		
		return new int[]{xi,yi};
	}
	
	public ILazyDataset getSpectrum(double x, double y) {
		int[] indices = getIndices(x, y);
		if (indices == null) return null;
		return parent.getSpectrum(indices[0], indices[1]);
	}
	
	public MappedData makeNewMapWithParent(String name, IDataset ds) {
		return new MappedData(name, ds, parent, path);
	}




	

	
}
