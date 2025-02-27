/*
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, Heinrich
 * Heine Universitaet Duesseldorf This software is licenced under EPL 1.0
 * (http://www.eclipse.org/org/documents/epl-v10.html)
 */

package de.prob.animator.domainobjects;

import java.util.List;
import java.util.stream.Collectors;

import de.be4.classicalb.core.parser.BParser;
import de.be4.classicalb.core.parser.analysis.prolog.ASTProlog;
import de.be4.classicalb.core.parser.exceptions.BCompoundException;
import de.be4.classicalb.core.parser.node.AExpressionParseUnit;
import de.be4.classicalb.core.parser.node.AIdentifierExpression;
import de.be4.classicalb.core.parser.node.APredicateParseUnit;
import de.be4.classicalb.core.parser.node.EOF;
import de.be4.classicalb.core.parser.node.Start;
import de.be4.classicalb.core.parser.node.TIdentifierLiteral;
import de.be4.classicalb.core.parser.util.PrettyPrinter;
import de.prob.model.representation.FormulaUUID;
import de.prob.model.representation.IFormulaUUID;
import de.prob.prolog.output.IPrologTermOutput;

/**
 * Representation of a ClassicalB formula.
 * 
 * @author joy
 */
public class ClassicalB extends AbstractEvalElement implements IBEvalElement {

	private final FormulaUUID uuid = new FormulaUUID();

	private final Start ast;
	private String code;

	public ClassicalB(final Start ast, final FormulaExpand expansion, final String code) {
		super(null, expansion);

		this.ast = ast;
		this.code = code;
	}

	public ClassicalB(final Start ast, final String code) {
		super(null, FormulaExpand.TRUNCATE);

		this.ast = ast;
		this.code = code;
	}

	public ClassicalB(final Start ast, final FormulaExpand expansion) {
		this(ast, expansion, null);
	}

	/**
	 * @param ast
	 *            is saved and the string representation determined from the ast
	 *            and saved
	 */
	public ClassicalB(final Start ast) {
		this(ast, FormulaExpand.TRUNCATE);
	}

	public ClassicalB(final String formula, final FormulaExpand expansion) {
		this(parse(formula,false), expansion, formula); // false: does not allow substitutions
	}

	/**
	 * @deprecated Parsing substitutions into an {@link IEvalElement} is not useful at the moment,
	 *     because they cannot be evaluated.
	 *     If you only need to work with expressions and predicates,
	 *     use {@link #ClassicalB(String)} instead.
	 *     To parse a substitution,
	 *     use {@link BParser#parseSubstitution(String)} instead
	 *     (you can then pass the parsed AST into {@link #ClassicalB(Start, String)} if you really need to for some reason).
	 */
	@Deprecated
	public ClassicalB(final String formula, final FormulaExpand expansion, final Boolean AllowSubst) {
		this(parse(formula,AllowSubst), expansion, formula);
	}

	/**
	 * @param code
	 *            will be parsed and the resulting {@link Start} ast saved
	 * @throws EvaluationException
	 *             if the code could not be parsed
	 */
	public ClassicalB(final String code) {
		this(code, FormulaExpand.EXPAND);
	}

	private static Start parse(final String formula, Boolean AllowSubst) {
		final BParser bParser = new BParser();
		try {
			return bParser.parseFormula(formula);
		} catch (BCompoundException e) {
		    if (AllowSubst) {
		       // also try parsing as substitution
				try {
					return bParser.parseSubstitution(formula);
				} catch (BCompoundException f) {
					throw new EvaluationException(f.getMessage(), f);
				}
			} else {
				throw new EvaluationException(e.getMessage(), e);
			}
		}
	}

	@Override
	public String getCode() {
		if (this.code == null) {
			final PrettyPrinter prettyPrinter = new PrettyPrinter();
			this.ast.apply(prettyPrinter);
			this.code = prettyPrinter.getPrettyPrint();
		}
		return this.code;
	}

	/**
	 * <p>Create a classical B formula representing the given identifier.</p>
	 * <p>
	 * Unlike the normal constructors that parse the input string using the B parser,
	 * this method accepts arbitrary strings as identifiers,
	 * even ones that are not syntactically valid B identifiers
	 * and would otherwise need to be quoted.
	 * </p>
	 * 
	 * @param identifier list of string parts that make up a dotted identifier
	 * @param expansion expansion mode to use when evaluating the formula
	 * @return a classical B formula representing the given identifier
	 */
	public static ClassicalB fromIdentifier(final List<String> identifier, final FormulaExpand expansion) {
		final AIdentifierExpression idNode = new AIdentifierExpression(identifier.stream().map(TIdentifierLiteral::new).collect(Collectors.toList()));
		final Start ast = new Start(new AExpressionParseUnit(idNode), new EOF());
		return new ClassicalB(ast, expansion);
	}

	/**
	 * <p>Create a classical B formula representing the given identifier.</p>
	 * <p>
	 * Unlike the normal constructors that parse the input string using the B parser,
	 * this method accepts arbitrary strings as identifiers,
	 * even ones that are not syntactically valid B identifiers
	 * and would otherwise need to be quoted.
	 * </p>
	 * 
	 * @param identifier list of string parts that make up a dotted identifier
	 * @return a classical B formula representing the given identifier
	 */
	public static ClassicalB fromIdentifier(final List<String> identifier) {
		return fromIdentifier(identifier, FormulaExpand.TRUNCATE);
	}

	@Override
	public EvalElementType getKind() {
		if (ast.getPParseUnit() instanceof AExpressionParseUnit) {
			return EvalElementType.EXPRESSION;
		} else if (ast.getPParseUnit() instanceof APredicateParseUnit) {
			return EvalElementType.PREDICATE;
		} else {
			return EvalElementType.ASSIGNMENT;
		}
	}

	/**
	 * @return {@link Start} ast corresponding to the formula
	 */
	@Override
	public Start getAst() {
		return ast;
	}

	@Override
	public void printProlog(final IPrologTermOutput pout) {
		if (EvalElementType.ASSIGNMENT.equals(getKind())) {
			throw new EvaluationException("Substitutions are currently unsupported for evaluation");
		}
		if (ast.getEOF() == null) {
			ast.setEOF(new EOF());
		}
		ASTProlog.printFormula(ast, pout);
	}

	@Override
	public IFormulaUUID getFormulaId() {
		return uuid;
	}
}
