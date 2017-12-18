package de.prob.animator.prologast;

import java.util.ArrayList;
import java.util.List;

import de.prob.parser.BindingGenerator;
import de.prob.prolog.term.ListPrologTerm;
import de.prob.prolog.term.PrologTerm;

/**
 * The left node is the category given by Prolog, the right node is a formula or
 * another category inside the root-category
 */
public class PrologAST {
	private List<PrologASTNode> astNodes;

	public PrologAST(ListPrologTerm nodes){
		this.astNodes = buildAST(nodes);
	}

	public List<PrologASTNode> getASTNodes() {
		return astNodes;
	}

	private List<PrologASTNode> buildAST(ListPrologTerm nodes) {
		List<PrologASTNode> categoryList = new ArrayList<>();
		for (int i = 0; i < nodes.size(); i++) {
			ASTCategory temp = (ASTCategory) makeASTNode(nodes.get(i));
			categoryList.add(temp);
		}
		return categoryList;
	}

	private PrologASTNode makeASTNode(PrologTerm node) {
		if (node.getFunctor().equals("formula")) {
			return new ASTFormula(node);
		} else if (node.getFunctor().equals("category")) {
			final List<PrologASTNode> subnodes = makeSubnodes(BindingGenerator.getList(node.getArgument(3)));
			final String name = node.getArgument(1).getFunctor();
			final boolean expanded = node.getArgument(2).toString().contains("expanded");
			final boolean propagated = node.getArgument(2).toString().contains("propagated");
			return new ASTCategory(subnodes, name, expanded, propagated);
		}
		return null;
	}

	private List<PrologASTNode> makeSubnodes(ListPrologTerm subnodes) {
		List<PrologASTNode> rightList = new ArrayList<>();
		for (PrologTerm m : subnodes) {
			rightList.add(makeASTNode(m));
		}
		return rightList;
	}
}
