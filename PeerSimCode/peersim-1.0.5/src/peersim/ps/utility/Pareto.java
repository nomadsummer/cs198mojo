package peersim.ps.utility;

import java.util.Random;


public class Pareto {
	double alpha;
	double xm;
	int seed = 0;
	Random rand = new Random();
	
	public Pareto(double alpha, int seed) {
		this.alpha = alpha;
		this.seed = seed;
	}

	public Pareto(double alpha, double xm) {
		this.alpha = alpha;
		this.xm = xm;
	}
	
	public double nextNumber() {

		double u = rand.nextDouble();
		
		return (xm / Math.pow(u, 1/alpha));
	}
	
	public double nextBoundedNumber(double l, double h) {

		double u = rand.nextDouble();
		double L = l;
		double H = h;
		
		double p;
		
		double a, b, c, d;
		
		if (L == 0)
			L = 0.01;
		
		a = u * Math.pow(H, alpha);
		b = u * Math.pow(L, alpha);
		c = Math.pow(H, alpha);
		d = Math.pow(L, alpha) * Math.pow(H, alpha);
		
		p = Math.pow(-(a-b-c) / d, -1 / alpha);
		
		return p;
		
	}
}