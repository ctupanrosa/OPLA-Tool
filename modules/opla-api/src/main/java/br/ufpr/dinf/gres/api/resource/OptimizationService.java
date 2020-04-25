package br.ufpr.dinf.gres.api.resource;

import br.ufpr.dinf.gres.api.dto.OptimizationDto;
import br.ufpr.dinf.gres.api.utils.Interactions;
import br.ufpr.dinf.gres.architecture.config.ApplicationFile;
import br.ufpr.dinf.gres.architecture.config.PathConfig;
import br.ufpr.dinf.gres.architecture.io.*;
import br.ufpr.dinf.gres.architecture.representation.Architecture;
import br.ufpr.dinf.gres.architecture.util.Constants;
import br.ufpr.dinf.gres.architecture.util.UserHome;
import br.ufpr.dinf.gres.core.jmetal4.core.Solution;
import br.ufpr.dinf.gres.core.jmetal4.core.SolutionSet;
import br.ufpr.dinf.gres.core.jmetal4.experiments.*;
import br.ufpr.dinf.gres.core.jmetal4.problems.OPLA;
import br.ufpr.dinf.gres.core.learning.ClusteringAlgorithm;
import br.ufpr.dinf.gres.domain.OPLAThreadScope;
import br.ufpr.dinf.gres.loglog.LogLog;
import br.ufpr.dinf.gres.loglog.LogLogData;
import br.ufpr.dinf.gres.loglog.Logger;
import br.ufpr.dinf.gres.patterns.strategies.scopeselection.impl.ElementsWithSameDesignPatternSelection;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.util.List;

@Service
public class OptimizationService {

    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(OptimizationService.class);
    private static final LogLog VIEW_LOGGER = Logger.getLogger();

    private final NSGAII_OPLA_FeatMutInitializer nsgaii;
    private final PAES_OPLA_FeatMutInitializer paes;
    private final InteractiveEmail interactiveEmail;

    public OptimizationService(NSGAII_OPLA_FeatMutInitializer nsgaii, PAES_OPLA_FeatMutInitializer paes, InteractiveEmail interactiveEmail) {
        this.nsgaii = nsgaii;
        this.paes = paes;
        this.interactiveEmail = interactiveEmail;
    }

    public Mono<OptimizationInfo> executeNSGAII(OptimizationDto optimizationDto) {
        String token = OPLAThreadScope.token.get();
        Thread thread = new Thread(() -> {
            OPLAThreadScope.token.set(token);
            OPLAThreadScope.mainThreadId.set(Thread.currentThread().getId());
            OPLAConfigThreadScope.userDir.set(optimizationDto.getConfig().getDirectoryToExportModels() + OPLAThreadScope.token.get() + System.getProperty("file.separator"));
            OPLAConfigThreadScope.pla.set(OPLAConfigThreadScope.userDir.get() + optimizationDto.getInputArchitecture());
            optimizationDto.getConfig().setPathToProfile(optimizationDto.getConfig().getDirectoryToExportModels() + OPLAThreadScope.token.get() + System.getProperty("file.separator") + optimizationDto.getConfig().getPathToProfile());
            optimizationDto.getConfig().setPathToProfileRelationships(optimizationDto.getConfig().getDirectoryToExportModels() + OPLAThreadScope.token.get() + System.getProperty("file.separator") + optimizationDto.getConfig().getPathToProfileRelationships());
            optimizationDto.getConfig().setPathToProfilePatterns(optimizationDto.getConfig().getDirectoryToExportModels() + OPLAThreadScope.token.get() + System.getProperty("file.separator") + optimizationDto.getConfig().getPathToProfilePatterns());
            optimizationDto.getConfig().setPathToProfileConcern(optimizationDto.getConfig().getDirectoryToExportModels() + OPLAThreadScope.token.get() + System.getProperty("file.separator") + optimizationDto.getConfig().getPathToProfileConcern());
            OPLAConfigThreadScope.setConfig(optimizationDto.getConfig());
            optimizationDto.setInputArchitecture(OPLAConfigThreadScope.pla.get());
            optimizationDto.setInteractiveFunction(solutionSet -> interactiveEmail.run(solutionSet, optimizationDto));
            executeNSGAIIAlgorithm(optimizationDto);
        });
        thread.start();
        return Mono.just(new OptimizationInfo(thread.getId(), "", OptimizationInfoStatus.RUNNING)).subscribeOn(Schedulers.elastic());
    }

    public Mono<OptimizationInfo> executePAES(OptimizationDto optimizationDto) {
        String token = OPLAThreadScope.token.get();
        Thread thread = new Thread(() -> {
            OPLAThreadScope.token.set(token);
            OPLAThreadScope.mainThreadId.set(Thread.currentThread().getId());
            OPLAConfigThreadScope.userDir.set(optimizationDto.getConfig().getDirectoryToExportModels() + OPLAThreadScope.token.get() + System.getProperty("file.separator"));
            OPLAConfigThreadScope.pla.set(OPLAConfigThreadScope.userDir.get() + optimizationDto.getInputArchitecture());
            optimizationDto.setInputArchitecture(OPLAConfigThreadScope.pla.get());
            optimizationDto.getConfig().setPathToProfile(optimizationDto.getConfig().getDirectoryToExportModels() + OPLAThreadScope.token.get() + System.getProperty("file.separator") + optimizationDto.getConfig().getPathToProfile());
            optimizationDto.getConfig().setPathToProfileRelationships(optimizationDto.getConfig().getDirectoryToExportModels() + OPLAThreadScope.token.get() + System.getProperty("file.separator") + optimizationDto.getConfig().getPathToProfileRelationships());
            optimizationDto.getConfig().setPathToProfilePatterns(optimizationDto.getConfig().getDirectoryToExportModels() + OPLAThreadScope.token.get() + System.getProperty("file.separator") + optimizationDto.getConfig().getPathToProfilePatterns());
            optimizationDto.getConfig().setPathToProfileConcern(optimizationDto.getConfig().getDirectoryToExportModels() + OPLAThreadScope.token.get() + System.getProperty("file.separator") + optimizationDto.getConfig().getPathToProfileConcern());
            OPLAConfigThreadScope.setConfig(optimizationDto.getConfig());
            optimizationDto.setInputArchitecture(OPLAConfigThreadScope.pla.get());
            optimizationDto.setInteractiveFunction(solutionSet -> interactiveEmail.run(solutionSet, optimizationDto));
            executePAESAlgorithm(optimizationDto);
        });
        thread.start();
        return Mono.just(new OptimizationInfo(OPLAThreadScope.mainThreadId.get(), "", OptimizationInfoStatus.RUNNING)).subscribeOn(Schedulers.elastic());
    }

    private void executeNSGAIIAlgorithm(OptimizationDto optimizationDto) {

        Logger.addListener(() -> {
            String s = LogLogData.printLog();
            if (OPLALogs.lastLogs.get(OPLAThreadScope.mainThreadId.get()) != null && OPLALogs.lastLogs.get(OPLAThreadScope.mainThreadId.get()).size() >= 100) {
                OPLALogs.lastLogs.get(OPLAThreadScope.mainThreadId.get()).clear();
            }
            OPLALogs.add(new OptimizationInfo(OPLAThreadScope.mainThreadId.get(), s, OptimizationInfoStatus.RUNNING));
        });

        LOGGER.info("set configuration path");
        ReaderConfig.setPathToConfigurationFile(UserHome.getPathToConfigFile());
        ReaderConfig.load();

        LOGGER.info("Create NSGA Config");
        NSGAIIConfig configs = new NSGAIIConfig();

        configs.setLogger(Logger.getLogger());
        configs.activeLogs();
        configs.setDescription(optimizationDto.getDescription());
        configs.setInteractive(optimizationDto.getInteractive());
        configs.setInteractiveFunction(optimizationDto.getInteractiveFunction());
        configs.setMaxInteractions(optimizationDto.getMaxInteractions());
        configs.setFirstInteraction(optimizationDto.getFirstInteraction());
        configs.setIntervalInteraction(optimizationDto.getIntervalInteraction());
        configs.setClusteringMoment(optimizationDto.getClusteringMoment());
        configs.setClusteringAlgorithm(optimizationDto.getClusteringAlgorithm());

        // Se mutação estiver marcada, pega os operadores selecionados ,e seta a probabilidade de mutacao
        if (optimizationDto.getMutation()) {
            LOGGER.info("Configure Mutation Operator");
            List<String> mutationsOperators = optimizationDto.getMutationOperators();
            configs.setMutationOperators(mutationsOperators);
            configs.setMutationProbability(optimizationDto.getMutationProbability());
        }

        configs.setPlas(optimizationDto.getInputArchitecture());
        configs.setNumberOfRuns(optimizationDto.getNumberRuns());
        configs.setPopulationSize(optimizationDto.getPopulationSize());
        configs.setMaxEvaluations(optimizationDto.getMaxEvaluations());

        // Se crossover estiver marcado, configura probabilidade
        // Caso contrario desativa
        if (optimizationDto.getCrossover()) {
            LOGGER.info("Configure Crossover Probability");
            configs.setCrossoverProbability(optimizationDto.getCrossoverProbability());
        } else {
            configs.disableCrossover();
        }

        // OPA-Patterns Configurations
        if (optimizationDto.getMutationOperators().contains(FeatureMutationOperators.DESIGN_PATTERNS.getOperatorName())) {
            // joao
            LOGGER.info("Instanciando o campo do Patterns - oplatool classe nsgaii");
            String[] array = new String[optimizationDto.getPatterns().size()];
            configs.setPatterns(optimizationDto.getPatterns().toArray(array));
            configs.setDesignPatternStrategy(new ElementsWithSameDesignPatternSelection(optimizationDto.getScopeSelection().get()));
        }

        List<String> operadores = configs.getMutationOperators();

        for (int i = 0; i < operadores.size(); i++) {
            if (operadores.get(i) == "DesignPatterns") {
                operadores.remove(i);
            }
        }
        configs.setMutationOperators(operadores);
        // operadores convencionais ok
        // operadores padroes ok
        // String[] padroes = configs.getPatterns();

        // Configura onde o br.ufpr.dinf.gres.opla.db esta localizado
        configs.setPathToDb(UserHome.getPathToDb());

        // Instancia a classe de configuracao da OPLA.java
        LOGGER.info("Create OPLA Config");
        OPLAConfigs oplaConfig = new OPLAConfigs();

        // Funcoes Objetivo
        oplaConfig.setSelectedObjectiveFunctions(optimizationDto.getObjectiveFunctions());

        // Add as confs de OPLA na classe de configuracoes gerais.
        configs.setOplaConfigs(oplaConfig);


        // Executa
        LOGGER.info("Execução NSGAII");
        try {
            nsgaii.run(configs);
        } catch (Exception e) {
            e.printStackTrace();
            OPLALogs.lastLogs.get(OPLAThreadScope.mainThreadId.get()).clear();
            OPLALogs.add(new OptimizationInfo(OPLAThreadScope.mainThreadId.get(), "ERROR", OptimizationInfoStatus.COMPLETE));
        }
        LOGGER.info("Fim Execução NSGAII");
        OPLALogs.lastLogs.get(OPLAThreadScope.mainThreadId.get()).clear();
        OPLALogs.add(new OptimizationInfo(OPLAThreadScope.mainThreadId.get(), "Fin", OptimizationInfoStatus.COMPLETE));
    }

    private void executePAESAlgorithm(OptimizationDto optimizationDto) {

        Logger.addListener(() -> {
            String s = LogLogData.printLog();
            if (OPLALogs.lastLogs.get(OPLAThreadScope.mainThreadId.get()) != null && OPLALogs.lastLogs.get(OPLAThreadScope.mainThreadId.get()).size() >= 100) {
                OPLALogs.lastLogs.get(OPLAThreadScope.mainThreadId.get()).clear();
            }
            OPLALogs.add(new OptimizationInfo(OPLAThreadScope.mainThreadId.get(), s, OptimizationInfoStatus.RUNNING));
        });

        ReaderConfig.setPathToConfigurationFile(UserHome.getPathToConfigFile());
        ReaderConfig.load();

        PaesConfigs configs = new PaesConfigs();
        configs.setDescription(optimizationDto.getDescription());

        //Se mutação estiver marcada, pega os operadores selecionados
        //,e seta a probabilidade de mutacao
        if (optimizationDto.getMutation()) {
            List<String> mutationsOperators = optimizationDto.getMutationOperators();
            configs.setMutationOperators(mutationsOperators);
            configs.setMutationProbability(optimizationDto.getMutationProbability());
        }

        configs.setPlas(optimizationDto.getInputArchitecture());
        configs.setNumberOfRuns(optimizationDto.getNumberRuns());
        configs.setMaxEvaluations(optimizationDto.getMaxEvaluations());
        configs.setArchiveSize(optimizationDto.getArchiveSize());


        //Se crossover estiver marcado, configura probabilidade
        //Caso contrario desativa
        if (optimizationDto.getCrossover()) {
            configs.setCrossoverProbability(optimizationDto.getCrossoverProbability());
        } else {
            configs.disableCrossover();
        }

        //OPA-Patterns Configurations
        if (optimizationDto.getMutationOperators().contains(FeatureMutationOperators.DESIGN_PATTERNS.getOperatorName())) {
            String[] array = new String[optimizationDto.getPatterns().size()];
            configs.setPatterns(optimizationDto.getPatterns().toArray(array));
            configs.setDesignPatternStrategy(new ElementsWithSameDesignPatternSelection(optimizationDto.getScopeSelection().get()));
        }

        //Configura onde o br.ufpr.dinf.gres.opla.db esta localizado
        configs.setPathToDb(UserHome.getPathToDb());

        //Instancia a classe de configuracao da OPLA.java
        OPLAConfigs oplaConfig = new OPLAConfigs();


        oplaConfig.setSelectedObjectiveFunctions(optimizationDto.getObjectiveFunctions());

        //Add as confs de OPLA na classe de configuracoes gerais.
        configs.setOplaConfigs(oplaConfig);

        //Executa
        try {
            paes.run(configs);
        } catch (Exception e) {
            e.printStackTrace();
            OPLALogs.lastLogs.get(OPLAThreadScope.mainThreadId.get()).clear();
            OPLALogs.add(new OptimizationInfo(OPLAThreadScope.mainThreadId.get(), "ERROR", OptimizationInfoStatus.COMPLETE));
        }
        LOGGER.info("Fim Execução NSGAII");
        OPLALogs.lastLogs.get(OPLAThreadScope.mainThreadId.get()).clear();
        OPLALogs.add(new OptimizationInfo(OPLAThreadScope.mainThreadId.get(), "Fin", OptimizationInfoStatus.COMPLETE));
    }

    public File downloadAlternative(Long threadId, Integer id) {
        SolutionSet solutionSet = Interactions.interactions.get(threadId).solutionSet;
        Solution solution = solutionSet.get(id);
        String plaNameOnAnalyses = "Interaction_" + OPLAThreadScope.currentGeneration.get() + "_" + solution.getAlternativeArchitecture().getName();
        String fileOnAnalyses = OPLAConfigThreadScope.config.get().getDirectoryToExportModels() + System.getProperty("file.separator") + plaNameOnAnalyses.concat(solutionSet.get(0).getAlternativeArchitecture().getName() + ".di");
//        File file = new File();
        return null;
    }

    public File downloadAllAlternative(Long threadId) {
        SolutionSet solutionSet = Interactions.interactions.get(threadId).solutionSet;
        for (int i = 0; i < solutionSet.size(); i++) {
            downloadAlternative(threadId, i);
        }
        PathConfig config = ApplicationFile.getInstance().getConfig();
        String url = config.getDirectoryToExportModels().toString().concat(OPLAThreadScope.hash.get());
        return new File(url);
    }
}
