package de.prob.ui.eventb;

import groovy.lang.Binding;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eventb.core.IContextRoot;
import org.eventb.core.IEventBRoot;
import org.eventb.core.IMachineRoot;
import org.eventb.core.ISCContextRoot;
import org.eventb.core.ISCMachineRoot;
import org.eventb.emf.persistence.ProjectResource;
import org.eventb.emf.core.Project;
import org.rodinp.core.IRodinFile;
import org.rodinp.core.IRodinProject;
import org.rodinp.core.RodinCore;

import com.google.inject.Injector;

import de.prob.animator.command.LoadEventBCommand;
import de.prob.animator.command.StartAnimationCommand;
import de.prob.model.eventb.EventBModel;
import de.prob.prolog.output.IPrologTermOutput;
import de.prob.statespace.History;
import de.prob.statespace.StateSpace;
import de.prob.webconsole.GroovyExecution;
import de.prob.webconsole.ServletContextListener;

public class StartAnimationHandler extends AbstractHandler {

	private ISelection fSelection;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		fSelection = HandlerUtil.getCurrentSelection(event);

		final IEventBRoot rootElement = getRootElement();

		Injector injector = ServletContextListener.INJECTOR;

		final EventBFactory instance = injector
				.getInstance(EventBFactory.class);

		IRodinProject rodinProject = rootElement.getRodinProject();
		ProjectResource resource = new ProjectResource(rodinProject);
		
		try {
			resource.load(null);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Project project = (Project) resource.getContents().get(0);
		String serialized = serialize(project);
		EventBModel model = null;
		try {
			model = instance.load(serialized, rootElement.getComponentName());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		StringWriter writer = new StringWriter();
		PrintWriter pto = new PrintWriter(writer);
		try {
			TranslatorFactory.translate(rootElement, pto);
		} catch (TranslationFailedException e) {
			e.printStackTrace();
		}

		StateSpace s = model.getStatespace();

		Pattern p2 = Pattern.compile("^package\\((.*?)\\)\\.");
		Matcher m2 = p2.matcher(writer.toString());
		m2.find();
		String cmd = m2.group(1);

		s.execute(new LoadEventBCommand(cmd));
		s.execute(new StartAnimationCommand());

		History h = new History(s);
		final GroovyExecution ge = injector.getInstance(GroovyExecution.class);
		Binding bindings = ge.getBindings();
		bindings.setVariable("defaultStateSpace", s);
		bindings.setVariable("defaultHistory", h);
		System.gc();
		
		return null;
	}

	
	private IEventBRoot getRootElement() {
		IEventBRoot root = null;
		if (fSelection instanceof IStructuredSelection) {
			final IStructuredSelection ssel = (IStructuredSelection) fSelection;
			if (ssel.size() == 1) {
				final Object element = ssel.getFirstElement();
				if (element instanceof IEventBRoot) {
					root = (IEventBRoot) element;
				} else if (element instanceof IFile) {
					IRodinFile rodinFile = RodinCore.valueOf((IFile) element);
					if (rodinFile != null)
						root = (IEventBRoot) rodinFile.getRoot();
				}
			}
		}
		return root;
	}

	public static void translate(final IEventBRoot root,
			final IPrologTermOutput pto) throws  {
		if (root instanceof IMachineRoot) {
			final ISCMachineRoot scRoot = ((IMachineRoot) root)
					.getSCMachineRoot();
			EventBMachineTranslator.create(scRoot, pto);

		} else if (root instanceof IContextRoot) {
			final ISCContextRoot scRoot = ((IContextRoot) root)
					.getSCContextRoot();
			EventBContextTranslator.create(scRoot, pto);
		} else {
			throw new TranslationFailedException(root.getComponentName(),
					"Cannot translate anything else than IMachineRoot or IContextRoot. Type was: "
							+ root.getClass());
		}
	}
}
