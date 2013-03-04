/*
 * Encog(tm) Core v3.2 - Java Version
 * http://www.heatonresearch.com/encog/
 * http://code.google.com/p/encog-java/
 
 * Copyright 2008-2012 Heaton Research, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *   
 * For more information on Heaton Research copyrights, licenses 
 * and trademarks visit:
 * http://www.heatonresearch.com/copyright
 */
package org.encog.neural.neat;

import java.io.Serializable;
import java.util.Random;

import org.encog.Encog;
import org.encog.engine.network.activation.ActivationFunction;
import org.encog.engine.network.activation.ActivationSteepenedSigmoid;
import org.encog.mathutil.randomize.factory.RandomFactory;
import org.encog.ml.MLError;
import org.encog.ml.MLRegression;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.ea.codec.GeneticCODEC;
import org.encog.ml.ea.genome.Genome;
import org.encog.ml.ea.population.BasicPopulation;
import org.encog.ml.ea.species.BasicSpecies;
import org.encog.ml.ea.species.Species;
import org.encog.neural.NeuralNetworkError;
import org.encog.neural.hyperneat.FactorHyperNEATGenome;
import org.encog.neural.hyperneat.HyperNEATCODEC;
import org.encog.neural.hyperneat.HyperNEATGenome;
import org.encog.neural.hyperneat.substrate.Substrate;
import org.encog.neural.neat.training.NEATGenome;
import org.encog.neural.neat.training.NEATInnovationList;
import org.encog.util.identity.BasicGenerateID;
import org.encog.util.identity.GenerateID;
import org.encog.util.obj.ChooseObject;

public class NEATPopulation extends BasicPopulation implements Serializable,
		MLError, MLRegression {

	/**
	 * The default survival rate.
	 */
	public static final double DEFAULT_SURVIVAL_RATE = 0.2;

	/**
	 * Property tag for the genomes collection.
	 */
	public static final String PROPERTY_GENOMES = "genomes";

	/**
	 * Property tag for the innovations collection.
	 */
	public static final String PROPERTY_INNOVATIONS = "innovations";

	public static final String PROPERTY_NEAT_ACTIVATION = "neatAct";

	/**
	 * Property tag for the population size.
	 */
	public static final String PROPERTY_POPULATION_SIZE = "populationSize";

	/**
	 * Property tag for the species collection.
	 */
	public static final String PROPERTY_SPECIES = "species";

	/**
	 * Property tag for the survival rate.
	 */
	public static final String PROPERTY_SURVIVAL_RATE = "survivalRate";

	/**
	 * Serial id.
	 */
	private static final long serialVersionUID = 1L;

	public static final int DEFAULT_CYCLES = 4;

	public static final String PROPERTY_CYCLES = "cycles";

	public static double clampWeight(final double w, final double weightRange) {
		if (w < -weightRange) {
			return -weightRange;
		} else if (w > weightRange) {
			return weightRange;
		} else {
			return w;
		}
	}

	private int activationCycles = NEATPopulation.DEFAULT_CYCLES;

	/**
	 * Generate gene id's.
	 */
	private final GenerateID geneIDGenerate = new BasicGenerateID();

	/**
	 * Generate innovation id's.
	 */
	private final GenerateID innovationIDGenerate = new BasicGenerateID();

	/**
	 * A list of innovations, or null if this feature is not being used.
	 */
	private NEATInnovationList innovations;

	private final double weightRange = 5;
	private Genome cachedBestGenome;
	private NEATNetwork bestNetwork;

	/**
	 * The number of input units. All members of the population must agree with
	 * this number.
	 */
	int inputCount;

	/**
	 * The number of output units. All members of the population must agree with
	 * this number.
	 */
	int outputCount;

	/**
	 * The survival rate.
	 */
	private double survivalRate = NEATPopulation.DEFAULT_SURVIVAL_RATE;

	private int maxIndividualSize = 100;

	private Substrate substrate;

	private final ChooseObject<ActivationFunction> activationFunctions = new ChooseObject<ActivationFunction>();

	private GeneticCODEC codec;

	private double initialConnectionDensity = 0.1;

	private RandomFactory randomNumberFactory = Encog.getInstance()
			.getRandomFactory().factorFactory();

	public NEATPopulation() {

	}

	/**
	 * Construct a starting NEAT population.
	 * 
	 * @param inputCount
	 *            The input neuron count.
	 * @param outputCount
	 *            The output neuron count.
	 * @param populationSize
	 *            The population size.
	 */
	public NEATPopulation(final int inputCount, final int outputCount,
			final int populationSize) {
		super(populationSize, null);
		this.inputCount = inputCount;
		this.outputCount = outputCount;

		setNEATActivationFunction(new ActivationSteepenedSigmoid());

		if (populationSize == 0) {
			throw new NeuralNetworkError(
					"Population must have more than zero genomes.");
		}

	}

	public NEATPopulation(final Substrate theSubstrate, final int populationSize) {
		super(populationSize, new FactorHyperNEATGenome());
		this.substrate = theSubstrate;
		this.inputCount = 6;
		this.outputCount = 2;
		HyperNEATGenome.buildCPPNActivationFunctions(this.activationFunctions);
	}

	public long assignGeneID() {
		return this.geneIDGenerate.generate();
	}

	public long assignInnovationID() {
		return this.innovationIDGenerate.generate();
	}

	@Override
	public double calculateError(final MLDataSet data) {
		updateBestNetwork();
		return this.bestNetwork.calculateError(data);
	}

	@Override
	public MLData compute(final MLData input) {
		updateBestNetwork();
		return this.bestNetwork.compute(input);
	}

	public int getActivationCycles() {
		return this.activationCycles;
	}

	/**
	 * @return the activationFunctions
	 */
	public ChooseObject<ActivationFunction> getActivationFunctions() {
		return this.activationFunctions;
	}

	/**
	 * @return the codec
	 */
	public GeneticCODEC getCODEC() {
		return this.codec;
	}

	/**
	 * @return the geneIDGenerate
	 */
	public GenerateID getGeneIDGenerate() {
		return this.geneIDGenerate;
	}

	/**
	 * @return the genomeFactory
	 */
	@Override
	public NEATGenomeFactory getGenomeFactory() {
		return (NEATGenomeFactory) super.getGenomeFactory();
	}

	/**
	 * @return the initialConnectionDensity
	 */
	public double getInitialConnectionDensity() {
		return this.initialConnectionDensity;
	}

	/**
	 * @return the innovationIDGenerate
	 */
	public GenerateID getInnovationIDGenerate() {
		return this.innovationIDGenerate;
	}

	public NEATInnovationList getInnovations() {
		return this.innovations;
	}

	/**
	 * @return the inputCount
	 */
	@Override
	public int getInputCount() {
		return this.inputCount;
	}

	@Override
	public int getMaxIndividualSize() {
		return this.maxIndividualSize;
	}

	/**
	 * @return the outputCount
	 */
	@Override
	public int getOutputCount() {
		return this.outputCount;
	}

	/**
	 * @return the randomNumberFactory
	 */
	public RandomFactory getRandomNumberFactory() {
		return this.randomNumberFactory;
	}

	public Substrate getSubstrate() {
		return this.substrate;
	}

	public double getSurvivalRate() {
		return this.survivalRate;
	}

	/**
	 * @return the weightRange
	 */
	public double getWeightRange() {
		return this.weightRange;
	}

	public boolean isHyperNEAT() {
		return this.substrate != null;
	}

	public void reset() {
		// create the genome factory
		if (isHyperNEAT()) {
			this.codec = new HyperNEATCODEC();
			setGenomeFactory(new FactorHyperNEATGenome());
		} else {
			this.codec = new NEATCODEC();
			setGenomeFactory(new FactorNEATGenome());
		}

		// create the new genomes
		getSpecies().clear();

		// reset counters
		getGeneIDGenerate().setCurrentID(1);
		getInnovationIDGenerate().setCurrentID(1);

		final Random rnd = this.randomNumberFactory.factor();

		// create one default species
		BasicSpecies defaultSpecies = new BasicSpecies();
		defaultSpecies.setPopulation(this);
		
		// create the initial population
		for (int i = 0; i < getPopulationSize(); i++) {
			final NEATGenome genome = getGenomeFactory().factor(rnd, this,
					this.inputCount, this.outputCount,
					this.initialConnectionDensity);
			defaultSpecies.add(genome);
		}
		defaultSpecies.setLeader(defaultSpecies.getMembers().get(0));
		this.getSpecies().add(defaultSpecies);

		// create initial innovations
		setInnovations(new NEATInnovationList(this));
	}

	public void setActivationCycles(final int activationCycles) {
		this.activationCycles = activationCycles;
	}

	/**
	 * @param codec
	 *            the codec to set
	 */
	public void setCODEC(final GeneticCODEC codec) {
		this.codec = codec;
	}

	/**
	 * @param initialConnectionDensity
	 *            the initialConnectionDensity to set
	 */
	public void setInitialConnectionDensity(
			final double initialConnectionDensity) {
		this.initialConnectionDensity = initialConnectionDensity;
	}

	public void setInnovations(final NEATInnovationList theInnovations) {
		this.innovations = theInnovations;
	}

	/**
	 * @param inputCount
	 *            the inputCount to set
	 */
	public void setInputCount(final int inputCount) {
		this.inputCount = inputCount;
	}

	public void setMaxIndividualSize(final int maxIndividualSize) {
		this.maxIndividualSize = maxIndividualSize;
	}

	public void setNEATActivationFunction(final ActivationFunction af) {
		this.activationFunctions.clear();
		this.activationFunctions.add(1.0, af);
		this.activationFunctions.finalizeStructure();
	}

	/**
	 * @param outputCount
	 *            the outputCount to set
	 */
	public void setOutputCount(final int outputCount) {
		this.outputCount = outputCount;
	}

	/**
	 * @param randomNumberFactory
	 *            the randomNumberFactory to set
	 */
	public void setRandomNumberFactory(final RandomFactory randomNumberFactory) {
		this.randomNumberFactory = randomNumberFactory;
	}

	/**
	 * @param substrate
	 *            the substrate to set
	 */
	public void setSubstrate(final Substrate substrate) {
		this.substrate = substrate;
	}

	public void setSurvivalRate(final double theSurvivalRate) {
		this.survivalRate = theSurvivalRate;
	}

	private void updateBestNetwork() {
		if( getBestGenome() != this.cachedBestGenome ) {
			this.cachedBestGenome = getBestGenome();
			this.bestNetwork = (NEATNetwork) this.getCODEC().decode(getBestGenome());
		}
	}

}
