package org.dawnsci.slicing.tools.hyper;

import java.util.List;

import org.eclipse.dawnsci.plotting.api.region.IRegion.RegionType;

import uk.ac.diamond.scisoft.analysis.dataset.IDataset;
import uk.ac.diamond.scisoft.analysis.dataset.ILazyDataset;
import uk.ac.diamond.scisoft.analysis.dataset.Slice;
import uk.ac.diamond.scisoft.analysis.roi.IROI;

/**
 * Interface for creating an object to reduce an ND array for display in the Hyperwindow
 * 
 * TODO FIXME Interfaces require Javadoc!
 */
public interface IDatasetROIReducer {

	boolean isOutput1D();
	
	IDataset reduce(ILazyDataset data, List<IDataset> axes, IROI roi, Slice[] slices, int[] order);
	
	List<RegionType> getSupportedRegionType();
	
	IROI getInitialROI(List<IDataset> axes, int[] order);
	
	boolean supportsMultipleRegions();
	
	List<IDataset> getAxes();
	
}
