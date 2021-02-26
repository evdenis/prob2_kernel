package de.prob.animator.command;

import java.util.Collections;
import java.util.List;

import de.prob.animator.CommandInterruptedException;
import de.prob.animator.IPrologResult;
import de.prob.animator.InterruptedResult;
import de.prob.animator.NoResult;
import de.prob.animator.YesResult;
import de.prob.animator.domainobjects.ErrorItem;
import de.prob.exception.ProBError;
import de.prob.parser.ISimplifiedROMap;
import de.prob.parser.ResultParserException;
import de.prob.prolog.output.IPrologTermOutput;
import de.prob.prolog.term.PrologTerm;
import de.prob.prolog.output.PrologTermStringOutput;

/**
 * The {@link AbstractCommand} class is used to implement composable
 * interactions with the ProB core. It defines two callback methods that are
 * being called by the Animator when the command is being performed. It also
 * provides a {@link #getSubcommands()} method to break down the command into
 * separate commands to improve debugging.
 * 
 * @author joy
 * 
 */
public abstract class AbstractCommand {
	/**
	 * Creates the prolog term that is sent to the core. It gets the term output
	 * object from the animator. The animator will automatically take care of
	 * name clashes when Prolog variables are used.
	 * 
	 * @param pto
	 *            {@link de.prob.prolog.output.IPrologTermOutput} that must be
	 *            used to write the query term.
	 */
	public abstract void writeCommand(IPrologTermOutput pto);

	/**
	 * <p>
	 * After performing the query this method receives a Map of bindings from
	 * variable names used in the query to Prolog terms representing the answer.
	 * </p>
	 * 
	 * <p>
	 * A number of helper tools can be used when processing the results (see
	 * {@link de.prob.parser.BindingGenerator})
	 * </p>
	 * 
	 * <p>
	 * This will be called if the Prolog query was successful and no error
	 * messages were logged during the execution of the query. If the query was
	 * not successful, or if there were errors
	 * {@link AbstractCommand#processErrorResult(IPrologResult, List)} will
	 * be called.
	 * </p>
	 * 
	 * <p>
	 * Note: This method is allowed to throw {@link ResultParserException} if
	 * the answer from Prolog does not match the expectation. The exception is a
	 * subclass of RuntimeException and it should always indicate a bug (or
	 * version inconsistency)
	 * </p>
	 * 
	 * @param bindings
	 *            {@link ISimplifiedROMap} of String variable names to their
	 *            calculated answers represented as {@link PrologTerm}s
	 */
	public abstract void processResult(
			ISimplifiedROMap<String, PrologTerm> bindings);

	/**
	 * Returns the list of sub-commands contained in a given
	 * {@link AbstractCommand}. This allow the animator to debug the code. If
	 * developers want individual commands to be executed separately in debug
	 * mode when an {@link AbstractCommand} is executed, then this method MUST
	 * be overridden. By default, {@link Collections#emptyList()} is returned.
	 * 
	 * @return {@code List} of {@link AbstractCommand} subcommands
	 */
	public List<AbstractCommand> getSubcommands() {
		return Collections.emptyList();
	}

	/**
	 * This code is called in three cases:
	 * <ol>
	 * <li>The Prolog query was unsuccessful (answered no) and there were no
	 * errors logged.</li>
	 * <li>The Prolog query was unsuccessful (answered no) and errors were found
	 * </li>
	 * <li>The Prolog query was successful (and bindings have been generated),
	 * but errors were also found</li>
	 * </ol>
	 * 
	 * Default behavior for error handling is implemented in
	 * {@link AbstractCommand}, but if a developer wants to implement special
	 * behavior, he/she needs to overwrite this method.
	 * 
	 * @param result the result returned from Prolog
	 * @param errors the error messages and locations that were reported
	 */
	public void processErrorResult(final IPrologResult result, final List<ErrorItem> errors) {
		if (result instanceof NoResult) {
			throw new ProBError("Prolog said no.", errors);
		} else if (result instanceof InterruptedResult) {
			throw new CommandInterruptedException("ProB was interrupted", errors);
		} else if (result instanceof YesResult) {
			processResult(((YesResult) result).getBindings());
			throw new ProBError("ProB reported Errors", errors);
		} else {
			throw new ProBError("Errors were", errors);
		}
	}
	
	/**
	 * This code is called when the Prolog process sends a progress term
	   but hasn't finished computation yet
	*/
	public void processProgressResult(final PrologTerm progressInfo) {
	   System.out.println("Progress info: " + progressInfo); // TO DO: replace by something more useful
	}
	
	
	/**
	 * This code is called when the Prolog process sends a call_back term
	   but hasn't finished computation yet
	*/
	public IPrologTermOutput processCallBack(final PrologTerm callBack) {
	    System.out.println("Callback request from Prolog: " + callBack);
	    
	    if (callBack.hasFunctor("interrupt_requested",0)) {
	       PrologTermStringOutput irq = new PrologTermStringOutput();
	       if (Thread.interrupted()) {
	          irq.printAtom("interrupt_is_requested");
	       } else {
	          irq.printAtom("not_requested");
	       }
	       System.out.println("irq: " + irq);
		   return irq;
	    }
		 // TO DO: provide way to deal with some call-backs: 
		 //  - parsing formulas (new ClassicalB(formulaToEval, FormulaExpand.EXPAND) ?)
	   
		PrologTermStringOutput callbackres = new PrologTermStringOutput();
		callbackres.printAtom("call_back_not_supported");
		return callbackres;
	}
}
