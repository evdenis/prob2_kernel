package de.prob.visualization;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.webconsole.servlets.visualizations.IVisualizationEngine;

@Singleton
public class VisualizationSelector {

	@Inject
	public VisualizationSelector() {
	}

	public void registerSession(final String sessionId,
			final IVisualizationEngine servlet) {
		// FIXME REFACTOR
	}

}
