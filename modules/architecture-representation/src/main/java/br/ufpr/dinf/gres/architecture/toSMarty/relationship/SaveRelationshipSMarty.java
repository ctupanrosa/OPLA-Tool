package br.ufpr.dinf.gres.architecture.toSMarty.relationship;

import br.ufpr.dinf.gres.architecture.representation.Architecture;
import br.ufpr.dinf.gres.architecture.toSMarty.SaveClassSMarty;

import java.io.PrintWriter;

/**
 * This class save all relationship from an architecture
 * Association, Dependency, Usage, Abstraction, Generalization, Realization, Requires
 * But save Usage and Abstraction as Dependency because SMarty Modeling not has this relationship in this moment
 */
public class SaveRelationshipSMarty {

    public SaveRelationshipSMarty() {
    }

    private static final SaveRelationshipSMarty INSTANCE = new SaveRelationshipSMarty();

    public static SaveRelationshipSMarty getInstance() {
        return INSTANCE;
    }

    /**
     * This class save all relationship from an architecture
     * Association, Dependency, Usage, Abstraction, Generalization, Realization, Requires
     *
     */
    public void Save(Architecture architecture, PrintWriter printWriter, String logPath) {
        SaveAssociationSMarty.getInstance().Save(architecture, printWriter, logPath);
        SaveDependencySMarty.getInstance().Save(architecture, printWriter, logPath);
        SaveUsageSMarty.getInstance().Save(architecture, printWriter, logPath);
        SaveAbstractionSMarty.getInstance().Save(architecture, printWriter, logPath);
        SaveGeneralizationSMarty.getInstance().Save(architecture, printWriter, logPath);
        SaveRealizationSMarty.getInstance().Save(architecture, printWriter, logPath);
        SaveRequiresSMarty.getInstance().Save(architecture, printWriter, logPath);
    }

}
