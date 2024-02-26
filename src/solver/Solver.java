package solver;

import com.github.lbfgs4j.LbfgsMinimizer;
import com.github.lbfgs4j.liblbfgs.Function;

public class Solver {
	private double[] table;
	
	public Solver() {
		Function f = new Function() {
			  public int getDimension() {
			    return 1;
			  }
			  public double valueAt(double[] x) {
			    return Math.pow(x[0]-5, 2) + 1;
			  }
			  public double[] gradientAt(double[] x) {
			    return new double[] { 2*(x[0]-5) };
			  }
			};
			
			LbfgsMinimizer minimizer = new LbfgsMinimizer();
			double[] x = minimizer.minimize(f); // x should be [5]
			double min = f.valueAt(x);          // min should be 1
	}
}
