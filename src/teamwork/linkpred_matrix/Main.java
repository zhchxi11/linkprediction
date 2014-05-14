package teamwork.linkpred_matrix;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.ml.distance.ManhattanDistance;
import org.apache.commons.math3.optim.PointValuePair;

public class Main {
	
	public static void main(String[] args) {
		/** Main */
		
		int n = 100;                                             // number of nodes
		int f = 2;                                               // number of features
		byte [][][] connected = new byte [1][][];
		connected[0] = Graph.generate(100);
		double [][][][] graphs = new double [1][n][n][];
		for (int i = 0; i < n; i++)
			for (int j = 0; j < n; j++)
				graphs[0][i][j] = Graph.generateFeatures(f);
		
		double alpha = 0.2;
		double b = 1; //1e-6;
		double lambda = 1;
		double [] parameters = {0.5, -0.3}; 
		byte [][] D = new byte [1][];
		D[0] = buildD(graphs[0], connected[0], 0, alpha, MatrixUtils.createRealVector(parameters), 10);
		int [] s = {0};
				
		LinkpredProblem problem = new LinkpredProblem(graphs, connected, s, D, alpha, lambda, b);
		problem.optimize();
		PointValuePair optimum = problem.getOptimum();
		
		System.out.println("Function minimum: " + optimum.getValue() + "\nParameters: " + 
		        optimum.getPoint()[0] + " " + optimum.getPoint()[1]);
	}
	
	
	public static byte [] buildD (double [][][] graph, byte [][] connected, 
			int s, double alpha, RealVector trueParameters, int topN) {
		/** Builds the D set (created links) for synthetic graph and known
		 *  parameter values, by taking the first topN highest ranked nodes.
		 *  s is the node whose links we are looking at
		 */
		int n = graph.length;
		byte [] D = new byte [n];
		int [] nodes = new int [n];
		for (int i = 1; i < n; i++)
			nodes[i] = i;
		
		// find pageranks
		double [][] A = buildAdjacencyMatrix(graph, trueParameters);
		RealMatrix Q = buildTransitionMatrix(A, connected, s, alpha);
		double [] rank = pagerank(Q);
		
		// sort the ranks
		double keyRank;
		int keyNode;
		int k;
		for (int i = 1; i < n; i++) {
		    keyRank = rank[i];
		    keyNode = nodes[i];
		    k = i - 1;
		    while (k >= 0 && rank[k] > keyRank) {
		    	rank[k + 1] = rank[k];
		    	nodes[k + 1] = nodes[k];
		        k--;
		    }
		    rank[k + 1] = keyRank;
		    nodes[k + 1] = keyNode;
		}
		
		// find top ranked nodes
		int count = topN;
		k = 0;
		while (k < n && count > 0) {
			if (nodes[k] != s && A[s][nodes[k]] == 1) {          // the node is not s, and has no links to s previously 
				D[nodes[k]] = 1;
				count--;
			}
			k++;
		}
		
		return D;		
	}
	
	private static double [][] buildAdjacencyMatrix (double [][][] graph, RealVector param) {
		/** Builds the adjacency matrix using exponential 
		 * edge-strength function, logistic function is the other option.
		 */
		int n = graph.length;
		double [][] A = new double [n][n];
		for (int i = 0; i < n; i++) 
			for (int j = 0; j < n; j++)
				A[i][j] = Math.exp(
						param.dotProduct(new ArrayRealVector(graph[i][j])));
		
		return A;
	}
	
	
	private static RealMatrix buildTransitionMatrix (double [][] A, byte [][] connected, int s, double alpha) {
		/** Builds the transition matrix for given adjacency matrix
		 *  and s as starting node
		 */  
		RealMatrix Q = new BlockRealMatrix(A.length, A.length);
		
		for (int i = 0; i < A.length; i++) {
			for (int j = 0; j < A.length; j++) {
				if (j == s)
					Q.setEntry(i, j, alpha);
				if (connected[i][j] == 1) 
					Q.setEntry(i, j, Q.getEntry(i, j) + 
						(1-alpha) * A[i][j] / sumElements(A[i]));
			}
		}
		
		return Q;
	}
	
	private static double sumElements (double [] a) {
		/** Sums the elements of an array */
		int sum = 0;
		for (int i = 0; i < a.length; i++)
			sum += a[i];
		return sum;
	}
	
	private static double [] pagerank (RealMatrix Q) {
		/** Calculates the pagerank, given a transition matrix */
		
		double EPSILON = 1e-12;
		ManhattanDistance manhattan = new ManhattanDistance();
		int n = Q.getRowDimension();
		double [] p = new double [n];
		double [] oldP = new double [n];
		
		for (int i = 0; i < n; i++)                      // pagerank initialization 
			p[i] = 1.0 / n;
		
		while (manhattan.compute(p, oldP) > EPSILON) {
			p = Q.preMultiply(p);
			oldP = p.clone();
		}
		
		return p;
	}
}




