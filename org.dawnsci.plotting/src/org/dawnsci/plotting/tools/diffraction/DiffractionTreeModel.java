package org.dawnsci.plotting.tools.diffraction;

import java.util.Map;
import java.util.TreeMap;

import javax.measure.quantity.Angle;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Quantity;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;
import javax.swing.tree.TreeNode;

import org.dawb.common.services.IImageService;
import org.dawb.common.services.ServiceManager;
import org.dawb.common.ui.plot.trace.IImageTrace;
import org.dawnsci.common.widgets.tree.AbstractNodeModel;
import org.dawnsci.common.widgets.tree.AmountEvent;
import org.dawnsci.common.widgets.tree.AmountListener;
import org.dawnsci.common.widgets.tree.LabelNode;
import org.dawnsci.common.widgets.tree.NumericNode;
import org.dawnsci.common.widgets.tree.ObjectNode;
import org.dawnsci.common.widgets.tree.UnitEvent;
import org.dawnsci.common.widgets.tree.UnitListener;
import org.eclipse.jface.viewers.TreeViewer;
import org.jscience.physics.amount.Amount;

import uk.ac.diamond.scisoft.analysis.diffraction.DetectorProperties;
import uk.ac.diamond.scisoft.analysis.diffraction.DetectorPropertyEvent;
import uk.ac.diamond.scisoft.analysis.diffraction.DiffractionCrystalEnvironment;
import uk.ac.diamond.scisoft.analysis.diffraction.IDetectorPropertyListener;
import uk.ac.diamond.scisoft.analysis.io.IDiffractionMetadata;
import uk.ac.diamond.scisoft.analysis.io.IMetaData;

/**
 * Holds data for the Diffraction model.
 * 
 * Use getNode(String labelPath)  to get a node for use in calculation actions.
 * 
 * The label path is the path to the value in label strings. It is not case
 * sensitive. For instance '/experimental information/beam center/X'  or
 * '/Experimental Information/Beam Center/X'
 * 
 * 
 * 
 * @author fcp94556
 *
 */
public class DiffractionTreeModel extends AbstractNodeModel {

    	
	private Unit<Length>               xpixel, ypixel;
	private NumericNode<Dimensionless> max,min,mean;
	private NumericNode<Length>        beamX, beamY;
	private final IDiffractionMetadata metaData;
	
	private boolean isActive=false;
	private NumericNode<Angle> yaw, pitch, roll;
	
	
	public DiffractionTreeModel(IDiffractionMetadata metaData) throws Exception {
		super();
		this.metaData = metaData;
		if (metaData.getDetector2DProperties()==null) throw new Exception("Must have detector properties!");
		if (metaData.getDiffractionCrystalEnvironment()==null) throw new Exception("Must have crystal environment!");
		createDiffractionModel(metaData);
	}
	
	public void activate() {
		this.isActive = true;
		DetectorProperties detector = getDetectorProperties();
		if (detector != null) {
			detector.addDetectorPropertyListener(detectorListener);
		}
	}
	
	public void deactivate() {
		this.isActive = false;
		DetectorProperties detector = getDetectorProperties();
		if (detector != null) {
			if (detectorListener != null)
				detector.removeDetectorPropertyListener(detectorListener);
		}
	}

	private void createDiffractionModel(IMetaData metaData) throws Exception {

		final DiffractionCrystalEnvironment  dce = getCrystalEnvironment();
		final DiffractionCrystalEnvironment odce = getOriginalCrystalEnvironment();
		final DetectorProperties         detprop = getDetectorProperties();
	    final DetectorProperties        odetprop = getOriginalDetectorProperties();
		
	    createExperimentalInfo(dce, odce, detprop, odetprop);
        
        createIntensity();      
        createDetector(dce, odce, detprop, odetprop);
        createRaw(metaData);
        
        createUnitsListeners(detprop, odetprop);
        
        // TODO listen to other things, for instance refine when it
        // is available may change other values.
        createDetectorListener(detprop);
	}
	
	private DetectorProperties getDetectorProperties() {
		return metaData.getDetector2DProperties();
	}

	private DetectorProperties getOriginalDetectorProperties() {
		return metaData.getOriginalDetector2DProperties();
	}

	private DiffractionCrystalEnvironment getCrystalEnvironment() {
		return metaData.getDiffractionCrystalEnvironment();
	}
	private DiffractionCrystalEnvironment getOriginalCrystalEnvironment() {
		return metaData.getOriginalDiffractionCrystalEnvironment();
	}

	private void createUnitsListeners(final DetectorProperties detprop, DetectorProperties odetprop) {
        if (detprop!=null) {
        	beamX.setDefault(getBeamX(odetprop));
        	beamX.setValue(getBeamX(detprop));
        	beamX.addAmountListener(new AmountListener<Length>() {		
    			@Override
    			public void amountChanged(AmountEvent<Length> evt) {
    				setBeamX(detprop, evt.getAmount());
    			}
    		});
        }
        beamX.setIncrement(0.01);
		beamX.setFormat("#0.##");
		beamX.setLowerBound(0);
		beamX.setUpperBound(100000);
        beamX.addUnitListener(createPixelFormatListener(beamX));
        
        if (detprop!=null) {
        	beamY.setDefault(getBeamY(odetprop));
        	beamY.setValue(getBeamY(detprop));
        	beamY.addAmountListener(new AmountListener<Length>() {		
    			@Override
    			public void amountChanged(AmountEvent<Length> evt) {
    				setBeamY(detprop, evt.getAmount());
    			}
    		});
        }
        beamY.setIncrement(0.01);
        beamY.setFormat("#0.##");
		beamY.setLowerBound(0);
		beamY.setUpperBound(100000);
        beamY.addUnitListener(createPixelFormatListener(beamY));		
	}

	private void createRaw(IMetaData metaData) throws Exception {
		
        if (metaData!=null && metaData.getMetaNames()!=null && metaData.getMetaNames().size()>0) {
            final LabelNode rawMeta = new LabelNode("Raw Meta", root);
	        registerNode(rawMeta);
        	for (String name : metaData.getMetaNames()) {
        		ObjectNode on = new ObjectNode(name, metaData.getMetaValue(name), rawMeta);
		        registerNode(on);
			}
        }		
	}

	private void createDetector(final DiffractionCrystalEnvironment dce, DiffractionCrystalEnvironment odce, final DetectorProperties detprop, DetectorProperties odetprop) {
       
		// Detector Meta
        final LabelNode detectorMeta = new LabelNode("Detector", root);
        registerNode(detectorMeta);
        detectorMeta.setDefaultExpanded(true);
        
        final NumericNode<Duration> exposure   = new NumericNode<Duration>("Exposure Time", detectorMeta, SI.SECOND);
        registerNode(exposure);
        if (dce!=null) {
           	exposure.setDefault(odce.getExposureTime(), SI.SECOND);
           	exposure.setValue(dce.getExposureTime(), SI.SECOND);
        }
        
        final LabelNode size = new LabelNode("Size", detectorMeta);
        registerNode(size);
        NumericNode<Length> x  = new NumericNode<Length>("x", size, SI.MILLIMETER);
        registerNode(x);
        if (detprop!=null) {
        	x.setDefault(odetprop.getDetectorSizeH(), SI.MILLIMETER);
        	x.setValue(detprop.getDetectorSizeH(), SI.MILLIMETER);
        }
        NumericNode<Length> y  = new NumericNode<Length>("y", size, SI.MILLIMETER);
        registerNode(y);
        if (detprop!=null) {
        	y.setDefault(odetprop.getDetectorSizeV(), SI.MILLIMETER);
        	y.setValue(detprop.getDetectorSizeV(), SI.MILLIMETER);
        }

        final LabelNode pixel = new LabelNode("Pixel", detectorMeta);
        registerNode(pixel);
        
        final NumericNode<Length> xPixelSize  = new NumericNode<Length>("x-size", pixel, SI.MILLIMETER);
        registerNode(xPixelSize);
        if (detprop!=null) {
           	xPixelSize.setDefault(odetprop.getHPxSize(), SI.MILLIMETER);
           	xPixelSize.setValue(detprop.getHPxSize(), SI.MILLIMETER);
        	xPixelSize.addAmountListener(new AmountListener<Length>() {				
				@Override
				public void amountChanged(AmountEvent<Length> evt) {
					detprop.setHPxSize(evt.getAmount().doubleValue(SI.MILLIMETER));
				}
			});
        }
        xPixelSize.setEditable(true);
        xPixelSize.setIncrement(0.01);
        xPixelSize.setFormat("#0.###");
        xPixelSize.setLowerBound(0.001);
        xPixelSize.setUpperBound(1000);

        final NumericNode<Length> yPixelSize  = new NumericNode<Length>("y-size", pixel, SI.MILLIMETER);
        registerNode(yPixelSize);
        if (detprop!=null) {
        	yPixelSize.setDefault(odetprop.getVPxSize(), SI.MILLIMETER);
        	yPixelSize.setValue(detprop.getVPxSize(), SI.MILLIMETER);
        	yPixelSize.addAmountListener(new AmountListener<Length>() {				
				@Override
				public void amountChanged(AmountEvent<Length> evt) {
					detprop.setVPxSize(evt.getAmount().doubleValue(SI.MILLIMETER));
				}
			});
        }
        yPixelSize.setEditable(true);
        yPixelSize.setIncrement(0.01);
        yPixelSize.setFormat("#0.###");
        yPixelSize.setLowerBound(0.001);
        yPixelSize.setUpperBound(1000);
        
	    // Beam Centre
        final LabelNode beamCen = new LabelNode("Beam Centre", detectorMeta);
        beamCen.setTooltip("The beam centre is the intersection of the direct beam with the detector in terms of image coordinates. Can be undefined when there is no intersection.");
        registerNode(beamCen);
        beamCen.setDefaultExpanded(true);

        beamX = new NumericNode<Length>("X", beamCen, SI.MILLIMETER);
        registerNode(beamX);
        beamX.setEditable(true);

        beamY = new NumericNode<Length>("Y", beamCen, SI.MILLIMETER);
        registerNode(beamY);
        beamY.setEditable(true);

        // Listeners
        xpixel = setBeamCenterUnit(xPixelSize, beamX, "pixel");
        xPixelSize.addAmountListener(new AmountListener<Length>() {		
			@Override
			public void amountChanged(AmountEvent<Length> evt) {
		        xpixel = setBeamCenterUnit(xPixelSize, beamX, "pixel");
			}
		});
        
        ypixel = setBeamCenterUnit(yPixelSize, beamY, "pixel");
        yPixelSize.addAmountListener(new AmountListener<Length>() {		
			@Override
			public void amountChanged(AmountEvent<Length> evt) {
				ypixel = setBeamCenterUnit(yPixelSize, beamY, "pixel");
			}
		});

        final LabelNode normal = new LabelNode("Normal (Orientation)", detectorMeta);
        normal.setTooltip("Detector orientation is defined by the normal to its face outward and towards the scattering sample. Rotations are centred on the beam centre");
        registerNode(normal);
        normal.setDefaultExpanded(true);
        
        final double[] oorientation = odetprop!=null ? odetprop.getNormalAnglesInDegrees() : null;
        final double[] orientation  = detprop!=null  ? detprop.getNormalAnglesInDegrees()  : null;
		yaw = createOrientationNode("Yaw", -180, 180, oorientation, orientation, 0, normal);
		yaw.setTooltip("Rotation about vertical axis, in degrees (positive is to the right).");
		pitch = createOrientationNode("Pitch", -90, 90, oorientation, orientation, 1, normal);
		pitch.setTooltip("Rotation about horizontal axis, in degrees (positive is upwards).");
		roll = createOrientationNode("Roll", -180, 180, oorientation, orientation, 2, normal);
		roll.setTooltip("Rotation about normal, in degrees (positive is clockwise).");
        
        
        final NumericNode<Length> dist = new NumericNode<Length>("Distance", detectorMeta, SI.MILLIMETER);
        dist.setTooltip("Distance from sample to beam centre");
        registerNode(dist);
        if (detprop!=null) {
        	dist.setDefault(odetprop.getBeamCentreDistance(), SI.MILLIMETER);
        	dist.setValue(detprop.getBeamCentreDistance(), SI.MILLIMETER);
            dist.addAmountListener(new AmountListener<Length>() {
    			@Override
    			public void amountChanged(AmountEvent<Length> evt) {
    				detprop.setBeamCentreDistance(evt.getAmount().doubleValue(SI.MILLIMETER));
    			}
    		});
        }
        dist.setEditable(true);

        dist.setIncrement(1);
        dist.setFormat("#0.##");
        dist.setLowerBound(0);
        dist.setUpperBound(1000000);
        final Unit<Length> micron = SI.MICRO(SI.METER);
        dist.setUnits(SI.MILLIMETER, micron, NonSI.INCH);

	}

	private void createIntensity() {
	       // Pixel Info
        final LabelNode pixelValue = new LabelNode("Intensity", root);
        registerNode(pixelValue);
        pixelValue.setDefaultExpanded(true);
				                 
        this.max  = new NumericNode<Dimensionless>("Visible Maximum", pixelValue, Dimensionless.UNIT);
        registerNode(max);
        this.min  = new NumericNode<Dimensionless>("Visible Minimum", pixelValue, Dimensionless.UNIT);
        registerNode(min);
        this.mean = new NumericNode<Dimensionless>("Mean", pixelValue, Dimensionless.UNIT);
        registerNode(mean);
 		
	}

	private void createExperimentalInfo(final DiffractionCrystalEnvironment dce,
			                                 DiffractionCrystalEnvironment odce, 
			                                 final DetectorProperties detprop, DetectorProperties odetprop) {
	    // Experimental Info
        final LabelNode experimentalInfo = new LabelNode("Experimental Information", root);
        registerNode(experimentalInfo);
        experimentalInfo.setDefaultExpanded(true);
       
        final NumericNode<Length> lambda = new NumericNode<Length>("Wavelength", experimentalInfo, NonSI.ANGSTROM);
        registerNode(lambda);
        if (dce!=null) {
           	lambda.setDefault(odce.getWavelength(), NonSI.ANGSTROM);
           	lambda.setValue(dce.getWavelength(), NonSI.ANGSTROM);
        	lambda.addAmountListener(new AmountListener<Length>() {		
				@Override
				public void amountChanged(AmountEvent<Length> evt) {
					dce.setWavelength(lambda.getValue(NonSI.ANGSTROM));
				}
			});
        }
        lambda.setEditable(true);
        lambda.setIncrement(0.01);
        lambda.setFormat("#0.####");
        lambda.setLowerBound(0);
        lambda.setUpperBound(1000); // It can be ev as well.
        lambda.setUnits(NonSI.ANGSTROM, NonSI.ELECTRON_VOLT, SI.KILO(NonSI.ELECTRON_VOLT));
        lambda.addUnitListener(new UnitListener() {	
			@Override
			public void unitChanged(UnitEvent<? extends Quantity> evt) {
				if (evt.getUnit().equals(NonSI.ANGSTROM)) {
			        lambda.setIncrement(0.01);
			        lambda.setFormat("#0.####");
			        lambda.setLowerBound(0);
			        lambda.setUpperBound(1000); // It can be ev as well.

				} else {
					lambda.setIncrement(0.01);
					lambda.setFormat("#0.##");
					lambda.setLowerBound(0);
					lambda.setUpperBound(100000);

				}
			}
		});
     
        NumericNode<Angle> start = new NumericNode<Angle>("Oscillation Start", experimentalInfo, NonSI.DEGREE_ANGLE);
        registerNode(start);
        if (dce!=null)  {
        	start.setDefault(odce.getPhiStart(), NonSI.DEGREE_ANGLE);
        	start.setValue(dce.getPhiStart(), NonSI.DEGREE_ANGLE);
        }
       
        NumericNode<Angle> stop = new NumericNode<Angle>("Oscillation Stop", experimentalInfo, NonSI.DEGREE_ANGLE);
        registerNode(stop);
        if (dce!=null)  {
        	stop.setDefault(odce.getPhiStart()+dce.getPhiRange(), NonSI.DEGREE_ANGLE);
        	stop.setValue(dce.getPhiStart()+dce.getPhiRange(), NonSI.DEGREE_ANGLE);
        }

        NumericNode<Angle> osci = new NumericNode<Angle>("Oscillation Range", experimentalInfo, NonSI.DEGREE_ANGLE);
        registerNode(osci);
        if (dce!=null)  {
        	osci.setDefault(odce.getPhiRange(), NonSI.DEGREE_ANGLE);
        	osci.setValue(dce.getPhiRange(), NonSI.DEGREE_ANGLE);
        }
	}

	private NumericNode<Angle> createOrientationNode(String label, 
			                                     int lower,
                                                 int upper,
			                                     double[] oorientation, 
			                                     double[] orientation, 
			                                     final int index,
			                                     LabelNode normal) {
		
		NumericNode<Angle> node = new NumericNode<Angle>(label, normal, NonSI.DEGREE_ANGLE);
        registerNode(node);
        if (orientation!=null)  {
        	node.setDefault(oorientation[index], NonSI.DEGREE_ANGLE);
        	node.setValue(orientation[index], NonSI.DEGREE_ANGLE);
        }
        node.setIncrement(1);
        node.setFormat("#0.##");
        node.setLowerBound(lower);
        node.setUpperBound(upper);
        node.setUnits(NonSI.DEGREE_ANGLE, SI.RADIAN);	
        node.setEditable(true);
        
        node.addAmountListener(new AmountListener<Angle>() {		
			@Override
			public void amountChanged(AmountEvent<Angle> evt) {
				double[] ori = getDetectorProperties().getNormalAnglesInDegrees();
				ori[index]   = evt.getAmount().doubleValue(NonSI.DEGREE_ANGLE);
//				System.err.printf("Node %d amount set: %f\n", index, ori[index]);
				getDetectorProperties().setNormalAnglesInDegrees(ori[0], ori[1], ori[2]);
			}
		});

        return node;
	}

	private Amount<Length> getBeamX(DetectorProperties dce) {
		final double[] beamCen = dce.getBeamCentreCoords();
		return Amount.valueOf(beamCen[0], xpixel);
	}
	
	private Amount<Length> getBeamY(DetectorProperties dce) {
		final double[] beamCen = dce.getBeamCentreCoords();
		return Amount.valueOf(beamCen[1], ypixel);
	}

	private void setBeamX(DetectorProperties dce, Amount<Length> beamX) {
		final double[] beamCen = dce.getBeamCentreCoords();
		beamCen[0] = beamX.doubleValue(xpixel);
		try {
			beamCenterActive = false;
			dce.setBeamCentreCoords(beamCen);
		} finally {
			beamCenterActive = true;
		}
	}
	private void setBeamY(DetectorProperties dce, Amount<Length> beamY) {
		final double[] beamCen = dce.getBeamCentreCoords();
		beamCen[1] = beamY.doubleValue(ypixel);
		try {
			beamCenterActive = false;
			dce.setBeamCentreCoords(beamCen);
		} finally {
			beamCenterActive = true;
		}
	}
	
	private IDetectorPropertyListener detectorListener;
	private boolean                   beamCenterActive=true;
	private void createDetectorListener(final DetectorProperties detprop) {
		if (detectorListener==null) this.detectorListener = new IDetectorPropertyListener() {
			
			@Override
			public void detectorPropertiesChanged(DetectorPropertyEvent evt) {
				if (!isActive)         return;
				if (evt.hasBeamCentreChanged()) {
					if (!beamCenterActive)
						return;
					double[]     cen = detprop.getBeamCentreCoords();
					Amount<Length> x = Amount.valueOf(cen[0], xpixel);
					beamX.setValueQuietly(x.to(beamX.getValue().getUnit()));
					if (viewer!=null) viewer.refresh(beamX); // Cancels cell editing.
					
					Amount<Length> y = Amount.valueOf(cen[1], ypixel);
					beamY.setValueQuietly(y.to(beamY.getValue().getUnit()));
					if (viewer!=null) viewer.refresh(beamY);  // Cancels cell editing.
				} else if (evt.hasNormalChanged()) {
					double[] angles = detprop.getNormalAnglesInDegrees();
//					System.err.printf("Detector: %f, %f, %f\n", angles[0], angles[1], angles[2]);
					yaw.setValueQuietly(angles[0], NonSI.DEGREE_ANGLE);
					pitch.setValueQuietly(angles[1], NonSI.DEGREE_ANGLE);
					roll.setValueQuietly(angles[2], NonSI.DEGREE_ANGLE);
					if (viewer != null) {
						viewer.update(new Object[] {yaw, pitch, roll}, null);
					}
				}
			}
		};
	}

	private UnitListener createPixelFormatListener(final NumericNode<Length> node) {
		return new UnitListener() {			
			@Override
			public void unitChanged(UnitEvent<? extends Quantity> evt) {
				if (evt.getUnit().toString().equals("pixel")) {
					node.setIncrement(1);
					node.setFormat("#0");
					node.setLowerBound(0);
					node.setUpperBound(100000);
				} else {
					node.setIncrement(0.01);
					node.setFormat("#0.##");
					node.setLowerBound(0);
					node.setUpperBound(1000);
				}
			}
		};	
	}

	protected Unit<Length> setBeamCenterUnit(NumericNode<Length> size,
			                                 NumericNode<Length> coord,
			                                 String unitName) {
		
        Unit<Length> unit = SI.MILLIMETER.times(size.getValue(SI.MILLIMETER));
        UnitFormat.getInstance().label(unit, unitName);
        coord.setUnits(SI.MILLIMETER, unit);
        if (viewer!=null) viewer.update(coord, new String[]{"Value","Unit"});
        return unit;
	}

	public void setIntensityValues(IImageTrace image) throws Exception {
		
		if (image==null)  return;
		if (isDisposed)   return;
		if (image.getImageServiceBean()==null) return;
		max.setDefault(image.getImageServiceBean().getMax().doubleValue(), Dimensionless.UNIT);
		min.setDefault(image.getImageServiceBean().getMin().doubleValue(), Dimensionless.UNIT);

		IImageService service = (IImageService)ServiceManager.getService(IImageService.class);
		float[] fa = service.getFastStatistics(image.getImageServiceBean());
		mean.setDefault(fa[2], Dimensionless.UNIT);
        mean.setLabel(image.getImageServiceBean().getHistogramType().getLabel());
	}

	public void dispose() {
		
		super.dispose();
		deactivate();
		
		final DetectorProperties detprop = getDetectorProperties();
		if (detprop != null) {
			if (detectorListener != null)
				detprop.removeDetectorPropertyListener(detectorListener);
		}
			
	}

}
