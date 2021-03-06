package org.dawnsci.mapping.ui.datamodel;

import java.util.Arrays;
import java.util.List;

import org.dawnsci.mapping.ui.LocalServiceManager;
import org.eclipse.dawnsci.analysis.api.dataset.IDataset;
import org.eclipse.dawnsci.analysis.api.dataset.ILazyDataset;
import org.eclipse.dawnsci.analysis.api.dataset.IRemoteDataset;
import org.eclipse.dawnsci.analysis.api.io.ILoaderService;
import org.eclipse.dawnsci.analysis.api.io.IRemoteDatasetService;
import org.eclipse.dawnsci.analysis.api.metadata.AxesMetadata;
import org.eclipse.dawnsci.analysis.api.monitor.IMonitor;
import org.eclipse.dawnsci.analysis.dataset.impl.Dataset;
import org.eclipse.dawnsci.analysis.dataset.impl.DatasetUtils;
import org.eclipse.dawnsci.analysis.dataset.metadata.AxesMetadataImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MappedFileFactory {

	private final static Logger logger = LoggerFactory.getLogger(MappedFileFactory.class);
	
	public static MappedDataFile getMappedDataFile(String path, AssociatedImage image) {
		
		MappedDataFile file = new MappedDataFile(path);
		file.addMapObject(image.toString(), image);
		
		return file;
		
	}
	
	public static MappedDataFile getMappedDataFile(String path, MappedDataFileBean bean, IMonitor monitor) {
		
		MappedDataFile file = new MappedDataFile(path);
		
		for (MappedBlockBean b : bean.getBlocks()) {
			String name = b.getName();
			if (monitor != null) {
				if (monitor.isCancelled()) return null;
				monitor.subTask(name);
			}
			MappedDataBlock block = setUpBlock(path, name, b, bean.getLiveBean());
			file.addMapObject(name, block);

			if (monitor != null) monitor.worked(1);
		}
		
		for (MapBean b : bean.getMaps()) {
			if (monitor != null) {
				if (monitor.isCancelled()) return null;
				monitor.subTask(b.getName());
				MappedDataBlock block = file.getDataBlockMap().get(b.getParent());
				AbstractMapData m = setUpMap(path, b.getName(),block, bean.getLiveBean());
//				m.getMap().setName(m.toString());
				file.addMapObject(b.getName(), m);
			}
		}
		
//		for (String map : maps){
//			
//			MappedData m = setUpMap(path, map,block, description);
//			file.addMapObject(map, m);
//		}
		
		return file;
	}
	
	
	private static MappedDataBlock setUpBlock(String path, String blockName, MappedBlockBean bean, LiveDataBean live) {
		MappedDataBlock block = null;
		
		List<String> axesNames = Arrays.asList(bean.getAxes());
		if (live != null) {
			IRemoteDataset lz = getRemoteDataset(path,blockName,live);
			LiveRemoteAxes remoteAxes = getRemoteAxes(axesNames, path, bean, live);
			block = new LiveMappedDataBlock(blockName, lz, bean.getxDim(), bean.getyDim(), path, remoteAxes);
			return block;
		}
		
		
		try {
			ILazyDataset lz = getLazyDataset(path,blockName);
			AxesMetadata axm = checkAndBuildAxesMetadata(axesNames, path, bean);
			lz.setMetadata(axm);
			block = new MappedDataBlock(blockName, lz, bean.getxDim(), bean.getyDim(), path);
		} catch (Exception e) {
			
		}
		
		return block;
	}
	
		
	private static AbstractMapData setUpMap(String path, String mapName, MappedDataBlock block, LiveDataBean live) {
		
		if (live != null && block instanceof LiveMappedDataBlock) {
			return new LiveMappedData(mapName, getRemoteDataset(path,mapName,live), (LiveMappedDataBlock)block, path);
		}
		
		
		try {
			ILazyDataset lz = getLazyDataset(path,mapName);
			
			IDataset d = null;
			if (live == null) {
				d = lz.getSlice();
				
				while (d.getRank() > 2) {
					d = ((Dataset)d).sum(d.getRank()-1);
					logger.warn("Summing " + mapName);
				}

			}
			
			ILazyDataset[] xAxis = block.getXAxis();
			ILazyDataset[] yAxis = block.getYAxis();
			
			ILazyDataset[] yView = new ILazyDataset[yAxis.length];
			ILazyDataset[] xView = new ILazyDataset[xAxis.length];
			
			for (int i = 0; i < xAxis.length; i++) xView[i] = xAxis[i] == null ? null : xAxis[i].getSliceView().squeezeEnds();
			for (int i = 0; i < yAxis.length; i++) yView[i] = yAxis[i] == null ? null : yAxis[i].getSliceView().squeezeEnds();

			if (block.isRemappingRequired() && d.getRank() == 1) {
				AxesMetadataImpl ax = new AxesMetadataImpl(1);
				ax.setAxis(0, xView);

				d.setMetadata(ax);
				return new ReMappedData(mapName, d, block, path);
			}

			AxesMetadataImpl ax = new AxesMetadataImpl(2);
			ax.setAxis(0,yView);
			ax.setAxis(1, xView);


			d.setMetadata(ax);

			return new MappedData(mapName,d,block,path);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}
	
	private static LiveRemoteAxes getRemoteAxes(List<String> axes, String path, MappedBlockBean bean, LiveDataBean live) {
		IRemoteDataset[] r = new IRemoteDataset[axes.size()];
		
		for (int i = 0; i < axes.size(); i++) {
			String s = axes.get(i);
			if (s != null) {
					r[i] = getRemoteDataset(path, s, live);
			}

		}
		
		LiveRemoteAxes lra = new LiveRemoteAxes(r);
		
		String s = bean.getxAxisForRemapping();
		if (s != null){
				lra.setxAxisForRemapping(getRemoteDataset(path, s, live));
		}
		
		return lra;
	}
	
	private static AxesMetadata checkAndBuildAxesMetadata(List<String> axes, String path, MappedBlockBean bean) {
		
		AxesMetadataImpl axm = null; 
		
		try {
			axm = new AxesMetadataImpl(axes.size());
			for (int i = 0; i < axes.size(); i++) {
				if (axes.get(i) == null) continue;
				ILazyDataset lz = getLazyDataset(path, axes.get(i));
				int[] ss = lz.getShape();
				
				if (ss.length == 1) {
					axm.addAxis(i, lz);
					
					String second = null;
					if (bean.getxDim() == i && bean.getxAxisForRemapping() != null) second = bean.getxAxisForRemapping();
					if (second != null) axm.addAxis(i, getLazyDataset(path, second));
					
				} else {
					//approximate 2D with 1D, should be done int the map/mapobjects
					IDataset ds = lz.getSlice();
					double min = ds.min().doubleValue();
					double max = ds.max().doubleValue();
					ILazyDataset s = DatasetUtils.linSpace(min, max, ss[i], Dataset.FLOAT64);
					
//					int[] start = new int[ss.length];
//					int[] stop = start.clone();
//					Arrays.fill(stop, 1);
//					int[] step = stop.clone();
//					SliceND slice = new SliceND(ss,start,stop,step);
//					slice.setSlice(i, 0, ss[i], 1);
//					ILazyDataset s = lz.getSlice(slice).squeeze();
					s.setName(axes.get(i));
					axm.addAxis(i, s);
				}
				
				
			}
			
		} catch (Exception e) {
			axm = null;
		}
		
		return axm;
		
	}
	
	private static ILazyDataset getLazyDataset(String path, String name, LiveDataBean lb) throws Exception {
		if (lb == null) {
			ILoaderService lService = LocalServiceManager.getLoaderService();
			return lService.getData(path, null).getLazyDataset(name);
		} 
		
		return getRemoteDataset(path, name, lb);
	}
	
	private static ILazyDataset getLazyDataset(String path, String name) throws Exception {
		return getLazyDataset(path, name, null);
	}
	
	private static IRemoteDataset getRemoteDataset(String path, String name, LiveDataBean lb) {
		IRemoteDatasetService rds = LocalServiceManager.getRemoteDatasetService();
		IRemoteDataset remote = rds.createRemoteDataset(lb.getHost(), lb.getPort());
		remote.setPath(path);
		remote.setDataset(name);
		
		return remote;
	}
	
	
}
