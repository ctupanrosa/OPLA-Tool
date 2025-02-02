package br.otimizes.oplatool.architecture.papyrus.touml;

import br.otimizes.oplatool.architecture.exceptions.CustonTypeNotFound;
import br.otimizes.oplatool.architecture.exceptions.InvalidMultiplicityForAssociationException;
import br.otimizes.oplatool.architecture.exceptions.NodeNotFound;
import br.otimizes.oplatool.architecture.representation.relationship.AssociationEnd;

/**
 * Composition operations
 *
 * @author edipofederle<edipofederle @ gmail.com>
 */
public class CompositionOperations {

    private final DocumentManager doc;
    private AssociationEnd client;
    private AssociationEnd target;
    private String name;

    public CompositionOperations(DocumentManager doc) {
        this.doc = doc;
    }

    public CompositionOperations createComposition() {
        return new CompositionOperations(doc);
    }

    public CompositionOperations between(AssociationEnd idElement) {
        this.client = idElement;
        return this;
    }

    public CompositionOperations and(AssociationEnd idElement) {
        this.target = idElement;
        return this;
    }

    public CompositionOperations withName(String name) {
        this.name = name;
        return this;
    }

    public void build() throws CustonTypeNotFound, NodeNotFound, InvalidMultiplicityForAssociationException {
        final AssociationNode cn = new AssociationNode(doc, null);
        Document.executeTransformation(doc, () -> cn.createAssociation(client, target, name, "composite"));
    }
}