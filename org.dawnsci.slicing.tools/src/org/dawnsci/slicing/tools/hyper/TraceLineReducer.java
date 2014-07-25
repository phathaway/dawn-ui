package org.dawnsci.slicing.tools.hyper;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.dawnsci.plotting.api.region.IRegion;
import org.eclipse.dawnsci.plotting.api.region.IRegion.RegionType;

import uk.ac.diamond.scisoft.analysis.dataset.Dataset;
import uk.ac.diamond.scisoft.analysis.dataset.DatasetFactory;
import uk.ac.diamond.scisoft.analysis.dataset.IDataset;
import uk.ac.diamond.scisoft.analysis.dataset.ILazyDataset;
import uk.ac.diamond.scisoft.analysis.dataset.Slice;
import uk.ac.diamond.scisoft.analysis.roi.IROI;
import uk.ac.diamond.scisoft.analysis.roi.LinearROI;
import uk.ac.diamond.scisoft.analysis.roi.ROISliceUtils;

public class TraceLineReducer implements IDatasetROIReducer {

	private final RegionType regionType = RegionType.LINE;
	private List<IDataset> traceAxes;
	
	
	@Override
	public IDataset reduce(ILazyDataset data, List<IDataset> axes,
			IROI roi, Slice[] slices, int[] order) {
		if (roi instanceof LinearROI) {
			final IDataset image = ROISliceUtils.getDataset(data, (LinearROI)roi, slices,new int[]{order[0],order[1]},1);
			
			IDataset length = DatasetFactory.createRange(image.getShape()[1], Dataset.INT32);
			length.setName("Line Length");
			
			this.traceAxes = new ArrayList<IDataset>();
			this.traceAxes.add(axes.get(2).getSlice());
			
			return image;
		}
		
		return null;
	}
	

	@Override
	public boolean isOutput1D() {
		return true;
	}

	@Override
	public List<RegionType> getSupportedRegionType() {
		
		List<IRegion.RegionType> regionList = new ArrayList<IRegion.RegionType>();
		regionList.add(regionType);
		
		return regionList;
	}
	
	@Override
	public IROI getInitialROI(List<IDataset> axes, int[] order) {

		
		int[] x = axes.get(0).getShape();
		int[] y = axes.get(1).getShape();
		
		double[] start = new double[]{0,0};
		double[] end = new double[]{x[0]/10,y[0]/10};
		
		return new LinearROI(start, end);

	}

	@Override
	public boolean supportsMultipleRegions() {
		return false;
	}

	@Override
	public List<IDataset> getAxes() {
		return traceAxes;
	}
}
