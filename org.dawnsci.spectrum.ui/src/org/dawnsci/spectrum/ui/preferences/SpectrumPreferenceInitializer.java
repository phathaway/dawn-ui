package org.dawnsci.spectrum.ui.preferences;

import org.dawnsci.spectrum.ui.Activator;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

public class SpectrumPreferenceInitializer extends
		AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		String xdatasetnames = "*energy;*Energy;Column_0;*RatioAbsorbance/x";
		String ydatasetnames = "*FFI0;*lnI0It;Column_1;*RatioAbsorbance/y;Number_of_Cores";
		
		store.setDefault(SpectrumConstants.X_DATASETS, xdatasetnames);
		store.setDefault(SpectrumConstants.Y_DATASETS, ydatasetnames);

	}

}