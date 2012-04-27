package org.dawnsci.rcp.functions.classes;


public class TwoXTransferFunction extends AbstractTransferFunction {

	@Override
	public double getPoint(double value) {
		double result = (2.0*value);
		if (result < 0) return 0.0;
		if (result > 1.0) return 1.0;
		return result;
	}


}
