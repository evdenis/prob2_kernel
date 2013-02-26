package de.bmotionstudio.core.editor;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveRegistry;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import de.bmotionstudio.core.util.PerspectiveUtil;
import de.prob.ui.PerspectiveFactory;

public class ProBStartup implements IStartup {

	@Override
	public void earlyStartup() {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				boolean deleted = false;
				IWorkbench workbench = PlatformUI.getWorkbench();
				IPerspectiveDescriptor currentPerspective = workbench
						.getActiveWorkbenchWindow().getActivePage()
						.getPerspective();
				IPerspectiveRegistry perspectiveRegistry = workbench
						.getPerspectiveRegistry();
				IPerspectiveDescriptor[] perspectives = perspectiveRegistry
						.getPerspectives();
				for (IPerspectiveDescriptor p : perspectives) {
					if (p.getId().replaceAll("<", "").startsWith("ProB_")) {
						PerspectiveUtil.closePerspective(p);
						PerspectiveUtil.deletePerspective(p);
						deleted = true;
					}
				}
				if (deleted
						&& currentPerspective.getId().replace("<", "")
								.startsWith("ProB_"))
					PerspectiveUtil
							.switchPerspective(PerspectiveFactory.PROB_PERSPECTIVE);
			}
		});
	}

}
