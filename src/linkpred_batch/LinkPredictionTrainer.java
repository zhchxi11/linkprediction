package linkpred_batch;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix1D;

public class LinkPredictionTrainer {
	private int g;                                               // number of graphs
	private int f;                                               // number of features
	private RandomWalkGraph [] graphs;                           // pageranker for each graph
	private double alpha;                                        // damping factor
	private double lambda;                                       // regularization parameter
	private double b;                                            // b parameter for the WMW loss function
	private double J;                                            // cost
	private double [] gradient;                                  // gradient
	private DoubleMatrix1D parameters;                           // parameters
	
	double [] previousPoint;                                     // TODO
		
	
	/**
	 * Constructor
	 * 
	 * @param graphs: training graphs
	 * @param f: number of features
	 * @param alpha: damping factor
	 * @param lambda: regularization parameter
	 * @param b: b parameter for the WMW loss function
	 */
	public LinkPredictionTrainer(RandomWalkGraph [] graphs, int f, double alpha, double lambda, double b) {		
		this.g = graphs.length;
		this.f = f;
		this.graphs = graphs;
		this.alpha = alpha;
		this.lambda = lambda;
		this.b = b;                                              // needed olnly if WMW loss function is used
		this.gradient = new double [f];
		this.J = Double.MAX_VALUE;
		
		previousPoint = new double [f];                          // TODO
		for (int i = 0; i < f; i++)
			previousPoint[i] = Math.random();
	}
	
	
	/**
	 * Calculates the Wilcoxon-Mann-Whitney loss function
	 * 
	 * @param x
	 * @return double
	 */
	public double WMWloss (double x) {
		double res = 1.0 / (1+ Math.exp(-x/b));
		return res;
	} // TODO: the loss functions might be put in a separate classes
	
	
	/**
	 * Calculates the derivative of the Wilcoxon-Mann-Whitney 
	 * loss function with respect to (pl - pd)
     * 
	 * @param x
	 * @return double
	 */
	public double WMWderivative (double x) {
		double tmp = 1.0 / (1+ Math.exp(x/b));
		double res = tmp * (1-tmp) / b; 
		return res;     		
	}
	
	
	/**
	 * Calculates the fitting error J 
	 * given initial parameter vector 
	 * 
	 * @param w: parameters for building the adjacency matrix
	 * @throws InterruptedException 
	 */
	public void costFunctionAndGradient (DoubleMatrix1D w) throws InterruptedException {			
		this.parameters = w;
		double regTerm = w.zDotProduct(w);                       // regularization term
		double errorTerm = 0;                                    // error term
		
		for (int i = 0; i < f; i++)                              // clear the gradient
			gradient[i] = 0;
		
		int num_threads = Runtime.getRuntime().availableProcessors()+1;             // TODO concurrency
		ThreadPoolExecutor executor = new ThreadPoolExecutor(num_threads,
				20, Long.MAX_VALUE, TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(g));  // TODO maximut thread pool from g to 20

		for (int k = 0; k < g; k++) { 
			final RandomWalkGraph graph = graphs[k];
		    executor.execute(new Runnable() {
									
				@Override
				public void run() {
					try {
						graph.pageRankAndGradient(parameters, alpha);       // for each graph 
					}
		            catch (Exception e) {
						e.printStackTrace();
					}
				}			
		    });
		}

		executor.shutdown();
		executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);
		
		int l, d;
		double delta;                                                                // pl - pd
		for (int k = 0; k < g; k++) {      		                                                            
			for (int i = 0; i < graphs[k].D.size(); i++) {                           // has link
				for (int j = 0; j < graphs[k].L.size(); j++) {                       // no link
					l = graphs[k].L.get(j);
					d = graphs[k].D.get(i);
					delta = graphs[k].p.get(l) - graphs[k].p.get(d);
										
					errorTerm += WMWloss(delta);
					
					for (int idx = 0; idx < f; idx++) {                              // for each element of the gradient vector
						gradient[idx] += (WMWderivative(delta) *                     // derivative of the error term
	    			    	    (graphs[k].dp[idx].get(l) - graphs[k].dp[idx].get(d)));					 
					}
				}
			}			
		}
		
	    J = regTerm + lambda * errorTerm;
	    
	    for (int idx = 0; idx < f; idx++) {
	    	gradient[idx] *= lambda;
			gradient[idx] += (2 * w.get(idx));                     // derivative of the regularization term		
			gradient[idx] *= 0.0001;                                 // TODO added learning rate
	    }
	}
	
	
	/**
	 * Calculates cost function and gradient
	 * and returns cost function value
	 * 
	 * @param w
	 * @return double
	 * @throws InterruptedException 
	 */
	public double getCost (double []  w)  {
		return J;
	}
	
	
	/**
	 * Returns the gradient of the cost function
	 * 
	 * @return double []
	 * @throws InterruptedException 
	 */
	public double [] getGradient (double [] w) throws InterruptedException {
		boolean newPoint = true;
		/*for (int i = 0; i < w.length; i++) {
			w[i] = roundDecimal(w[i], 5);
			if (!(w[i] > previousPoint[i] || w[i] < previousPoint[i])) {
				newPoint = false;
				break;
			}
		}*/
		
		if (newPoint)
			costFunctionAndGradient(new DenseDoubleMatrix1D(w));
		return gradient;
	}
	
	
	private double roundDecimal (double num, int decimal) {
		double multiplier = Math.pow(10, decimal);
		num = num*multiplier;
		num = Math.round(num);
		num /= multiplier;
		return num;
	}
	
	
	public int getParametersNumber () {
		return f;
	}
}