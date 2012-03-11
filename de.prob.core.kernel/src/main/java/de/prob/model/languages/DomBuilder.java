package de.prob.model.languages;

import java.util.ArrayList;
import java.util.LinkedList;

import com.google.common.base.Joiner;
import com.google.inject.Inject;

import de.be4.classicalb.core.parser.analysis.DepthFirstAdapter;
import de.be4.classicalb.core.parser.node.AConstantsMachineClause;
import de.be4.classicalb.core.parser.node.AIdentifierExpression;
import de.be4.classicalb.core.parser.node.AMachineHeader;
import de.be4.classicalb.core.parser.node.AVariablesMachineClause;
import de.be4.classicalb.core.parser.node.PExpression;
import de.be4.classicalb.core.parser.node.Start;
import de.be4.classicalb.core.parser.node.TIdentifierLiteral;

public class DomBuilder extends DepthFirstAdapter {

	private final ClassicalBMachine machine;

	@Inject
	public DomBuilder(final ClassicalBMachine machine) {
		this.machine = machine;
	}

	public ClassicalBMachine build(final Start ast) {
		ast.apply(this);
		return getMachine();
	}

	@Override
	public void outAMachineHeader(final AMachineHeader node) {
		getMachine().setName(extractIdentifierName(node.getName()));
	}

	private String extractIdentifierName(
			final LinkedList<TIdentifierLiteral> nameL) {
		String text;
		if (nameL.size() == 1) {
			text = nameL.get(0).getText();
		} else {
			ArrayList<String> list = new ArrayList<String>();
			for (TIdentifierLiteral t : nameL) {
				list.add(t.getText());
			}
			text = Joiner.on(".").join(list);
		}
		return text;
	}

	@Override
	public void outAVariablesMachineClause(final AVariablesMachineClause node) {
		LinkedList<PExpression> identifiers = node.getIdentifiers();
		for (PExpression pExpression : identifiers) {
			if (pExpression instanceof AIdentifierExpression) {
				AIdentifierExpression id = (AIdentifierExpression) pExpression;
				String name = extractIdentifierName(id.getIdentifier());
				getMachine().addVariable(new NamedEntity(name, id));
			}
		}
	}

	@Override
	public void outAConstantsMachineClause(final AConstantsMachineClause node) {
		LinkedList<PExpression> identifiers = node.getIdentifiers();
		for (PExpression pExpression : identifiers) {
			if (pExpression instanceof AIdentifierExpression) {
				AIdentifierExpression id = (AIdentifierExpression) pExpression;
				String name = extractIdentifierName(id.getIdentifier());
				getMachine().addConstant(new NamedEntity(name, id));
			}
		}
	}

	public ClassicalBMachine getMachine() {
		return machine;
	}

}
