package linkpred_batch;

import org.apache.commons.math3.optim.PointValuePair;

import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix1D;

public class MultiplexMain {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Graph generation start");           //TODO
		
		// TODO the optimizator throws an exception with 50 graphs, no exception for 20 or less
		int g = 1;                                              // number of graphs   
		int n = 100;                                            // number of nodes    
		int f1 = 2;                                             // number of features for the first graph
		int f2 = 3;                                             // numer of features for the second graph
		
		int s = 0;                                              // the node whose links we learn, in this case 0 for each graph
		double alpha = 0.1;                                     // damping factor
		double interlayer = 0.2;                                // interlayer jump coeffiecient
		double b = 1e-6;                                        // WMW function parameter
		double lambda = 1;                                      // regularization parameter 
		
		double [] param = {1, -1, 0.5, -2, 1};                  // parameters vector 2 for the first graph, 3 for the second
		DoubleMatrix1D parameters = new 
				DenseDoubleMatrix1D(param);	
		DoubleMatrix1D parameters1 = new 
				DenseDoubleMatrix1D(new double [] {1, -1});	
		DoubleMatrix1D parameters2 = new 
				DenseDoubleMatrix1D(new double [] {0.5, -2, 1});	
				
		int topN = 5;
		
		Network [] graphs = new Network [2];   // build the graph
		ArtifitialGraphGenerator.initialize(f1);
		graphs[0] = (Network) ArtifitialGraphGenerator.generate(n, f1, s, topN, parameters1, alpha + interlayer);
		ArtifitialGraphGenerator.initialize(f2);
		graphs[1] = (Network) ArtifitialGraphGenerator.generate(n, f2, s, topN, parameters2, alpha + interlayer);
		
		MultiplexNetwork multiplex = new MultiplexNetwork(graphs, interlayer);
		
		// TODO TEST
		//graphs[0].buildAdjacencyMatrix(parameters1);
		//graphs[1].buildAdjacencyMatrix(parameters2);
		//multiplex.buildAdjacencyMatrix(parameters);
		//multiplex.printMatrix(graphs[0].buildTransitionTranspose(alpha));
		//multiplex.printMatrix(graphs[1].buildTransitionTranspose(alpha));
		//multiplex.printMatrix(multiplex.buildTransitionTranspose(alpha));
		
		//multiplex.isColumnStochastic(graphs[0].buildTransitionTranspose(alpha));
		//multiplex.isColumnStochastic(graphs[1].buildTransitionTranspose(alpha));
		//multiplex.isColumnStochastic(multiplex.buildTransitionTranspose(alpha));
		// TEST
		
		
		
		System.out.println("Graph generation end");			   //TODO	
		
		
		// GRADIENT DESCENT OPTIMIZATION START
		
		long start = System.nanoTime();
		
		int maxIterations = 500;
		int restarts = 50;
		double gradientTreshold = 1e-4;
		double costThreshold = 6;
		double [] initialParameters = new double [f1 + f2];
		for (int i = 0; i < f1 + f2; i++)
			initialParameters[i] = Math.random();
		
		GradientDescent gd = new GradientDescent(new LinkPredictionTrainer(
				new RandomWalkGraph [] {multiplex}, f1+f2, alpha, lambda, b), 
				maxIterations, 
				gradientTreshold, 
				costThreshold);
		
		PointValuePair optimum = null;
		try {
			//optimum = gd.optimize(initialParameters);
			optimum = gd.multiStartOptimize(restarts, initialParameters);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		long end = System.nanoTime();
		
		// GRADIENT DESCENT OPTIMIZATION END
		
		System.out.println(gd.getStopReason());
		System.out.println("Function minimum: " + optimum.getValue() + "\nParameters: ");
		for (int i = 0; i < f1+f2; i++)
		        System.out.print(optimum.getPoint()[i] + " ");
		System.out.println("\nResults in in " + (end-start)/60E9 + " minutes.");
		

	}

}
