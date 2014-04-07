package org.dawnsci.spectrum.ui.processing;

import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.optimize.ApachePolynomial;

public class PolySmoothProcess extends AbstractProcess {

	@Override
	protected AbstractDataset process(AbstractDataset x, AbstractDataset y) {
		try {
			return ApachePolynomial.getPolynomialSmoothed(x,y,13,9);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}

	@Override
	protected String getAppendingName() {
		return "_smooth";
	}

}