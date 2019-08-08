package de.prob.animator.command;

import de.prob.parser.ISimplifiedROMap;
import de.prob.prolog.output.IPrologTermOutput;
import de.prob.prolog.term.PrologTerm;

public class LoadZCommand extends AbstractCommand {
	private static final String PROLOG_COMMAND_NAME = "load_z_spec_from_tex_file";

	private final String path;

	public LoadZCommand(final String path) {
		this.path = path;
	}

	@Override
	public void writeCommand(final IPrologTermOutput pto) {
		pto.openTerm(PROLOG_COMMAND_NAME);
		pto.printAtom(path);
		pto.closeTerm();
	}

	@Override
	public void processResult(final ISimplifiedROMap<String, PrologTerm> bindings) {
		// There are no output variables.
	}
}
