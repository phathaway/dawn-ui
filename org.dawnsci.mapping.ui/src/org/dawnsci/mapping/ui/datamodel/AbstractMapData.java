package org.dawnsci.mapping.ui.datamodel;

import org.eclipse.dawnsci.analysis.api.dataset.IDataset;
import org.eclipse.dawnsci.analysis.api.dataset.ILazyDataset;

public abstract class AbstractMapData implements PlottableMapObject{

	private String name;
	protected String path;
	protected ILazyDataset baseMap;
	protected IDataset map;
	protected MappedDataBlock oParent;
	protected MappedDataBlock parent;
	private int transparency = -1;
	private double[] range;
	
	public AbstractMapData(String name, IDataset map, MappedDataBlock parent, String path) {
		this.name = name;
		this.map = map;
		this.path = path;
		this.oParent = this.parent = parent;
		range = calculateRange(map);
	}
	
	public AbstractMapData(String name, ILazyDataset map, MappedDataBlock parent, String path) {
		this.name = name;
		this.baseMap = map;
		this.path = path;
		this.oParent = this.parent = parent;
		range = calculateRange(map);
	}
	
	public abstract ILazyDataset getSpectrum(double x, double y);
	
	public MappedData makeNewMapWithParent(String name, IDataset ds) {
		return new MappedData(name, ds, parent, path);
	}
	
	public IDataset getData(){
		
		if (baseMap != null) {
			return baseMap.getSlice();
		}
		
		return map;
	}
	
	protected abstract double[] calculateRange(ILazyDataset map);
	
	protected void setRange(double[] range) {
		this.range = range;
	}
	
	@Override
	public String toString() {
		return name;
	}

	@Override
	public boolean hasChildren() {
		return false;
	}

	@Override
	public Object[] getChildren() {
		return null;
	}

	public int getTransparency() {
		return transparency;
	}

	public void setTransparency(int transparency) {
		this.transparency = transparency;
	}

	public MappedDataBlock getParent() {
		return parent;
	}

	public void setParent(MappedDataBlock parent) {
		this.parent = parent;
	}

	public void resetParent() {
		parent = oParent;
	}

	@Override
	public double[] getRange() {
		return range == null ? null : range.clone();
	}
	
	public String getLongName() {
		return path + " : " + name;
	}
	
}
