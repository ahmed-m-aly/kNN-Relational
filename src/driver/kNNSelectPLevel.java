package driver;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.SortedMap;

import optimizer.CostEstimator;

import output.kNNToken;

import data.*;

import index.*;
import exec.*;

public class kNNSelectPLevel {

	private static String path = "//Users//ahmed//Desktop//kNNRelational Experimental Results//final//Select//P-Level//";			
	private static Tuple.Location qLocation;

	private static Rectangle bounds;

	public static void main(String[] args) {

		Tuple t = new Tuple();
		double mapScale = 100000000;
		t.location.xCoord = 3.7529 * mapScale;
		t.location.yCoord = 5.5766 * mapScale;
		qLocation = t.location;

		bounds = new Rectangle(Constants.minLong, Constants.minLat, Constants.maxLong-Constants.minLong, Constants.maxLat-Constants.minLat);
		
		//runAllSF();
		//runAllK();
		runAllPriceSelectivity();
		//runAllCountSelectivity();
	}

	private static void runAllSF() {

		int k = 10;

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(path + "SF.csv"));
			out.write("Scale factor, k, Price selectivity, Count selectivity, Relational First, kNN First, Optimized\r\n");
			out.flush();

			for (int sf = 1; sf <= 10; sf ++) {

				QuadTree customerQTree = new QuadTree(bounds);
				HashMap<Integer, Customer> customerHashMap = new HashMap<Integer, Customer>();

				BPTree<Double, ArrayList<Order>> orderBPTree = new BPTree<Double, ArrayList<Order>>();		
				HashMap<Integer, ArrayList<Order>> orderHashMap =  new HashMap<Integer, ArrayList<Order>>();

				readData(sf, customerQTree, customerHashMap, orderBPTree, orderHashMap);

				System.gc();

				int countThreshold = 10;
				double countSelectivity = getCountSelectivity(countThreshold, orderHashMap, customerHashMap);
				int priceThreshold = 91000;

				SortedMap<Double, ArrayList<Order>> sm = orderBPTree.subMap(orderBPTree.firstKey(), (double)priceThreshold);
				double priceSelectivity = 1 - ((double)sm.size() / orderBPTree.size());

				System.out.println(sf + ", " + k + "," + priceSelectivity + "," + countSelectivity);
				System.out.println("priceThreshold = " + priceThreshold + ", countThreshold " + countThreshold);

				out.write(sf + ", " + k + "," + priceSelectivity + "," + countSelectivity + ",");
				run(out, k, priceThreshold, countThreshold, qLocation, customerQTree, customerHashMap, orderBPTree, orderHashMap, priceSelectivity, countSelectivity);


				out.flush();
			}

			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// customerQTree indexes the customers through a quad tree where the location is the key.
	// customerHashMap indexes the customers through a hash map where the customer id is the key.
	// orderBPTree indexes the orders through a B+Tree on the total price.
	// ordersHashMap indexes the orders through a hash map where the customer id is the key.
	private static void readData(int scaleFactor, QuadTree customerQTree, HashMap<Integer, Customer> customerHashMap,
			BPTree<Double, ArrayList<Order>> orderBPTree, HashMap<Integer, ArrayList<Order>> orderHashMap) {
		System.out.println("Reading Data");
		String path = "//Users//ahmed//Documents//MyWork//data//TPC-H//" + scaleFactor + "//";
		DataReader reader = new DataReader(path);

		reader.readOrdersIndexedByPriceAndCustID(orderBPTree, orderHashMap);
		reader.readCustomersIndexedByLocationAndCustID(customerQTree, customerHashMap);

		QuadTree enclosingNode = customerQTree.searchEnclosingLeaf(qLocation);

		CostEstimator.estimate(enclosingNode, customerQTree);

	}

	private static void runAllK() {

		int scaleFactor = 1;

		QuadTree customerQTree = new QuadTree(bounds);
		HashMap<Integer, Customer> customerHashMap = new HashMap<Integer, Customer>();

		BPTree<Double, ArrayList<Order>> orderBPTree = new BPTree<Double, ArrayList<Order>>();		
		HashMap<Integer, ArrayList<Order>> orderHashMap =  new HashMap<Integer, ArrayList<Order>>();

		readData(scaleFactor, customerQTree, customerHashMap, orderBPTree, orderHashMap);

		System.gc();

		System.out.println("Starting Execution");

		double priceThreshold = 2000;
		SortedMap<Double, ArrayList<Order>> sm = orderBPTree.subMap(orderBPTree.firstKey(), (double)priceThreshold);
		double priceSelectivity = 1 - ((double)sm.size() / orderBPTree.size());
		System.out.println("priceSelectivity = " + priceSelectivity);

		int countThreshold = 10;
		double countSelectivity = getCountSelectivity(countThreshold, orderHashMap, customerHashMap);

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(path + "k.csv"));
			out.write("Scale factor, k, Price selectivity, Count selectivity, Relational First, kNN First, Optimized\r\n");

			out.flush();

			for (int k = 2; k <= 131072; k*=2) {

				System.out.println(scaleFactor + ", " + k + "," + priceSelectivity + "," + countSelectivity);
				System.out.println("priceThreshold = " + priceThreshold + ", countThreshold " + countThreshold);

				out.write(scaleFactor + ", " + k + "," + priceSelectivity + "," + countSelectivity + ",");
				run(out, k, priceThreshold, countThreshold, qLocation, customerQTree, customerHashMap, orderBPTree, orderHashMap, priceSelectivity, countSelectivity);
			}

			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void runAllPriceSelectivity() {

		int scaleFactor = 1;

		QuadTree customerQTree = new QuadTree(bounds);
		HashMap<Integer, Customer> customerHashMap = new HashMap<Integer, Customer>();

		BPTree<Double, ArrayList<Order>> orderBPTree = new BPTree<Double, ArrayList<Order>>();		
		HashMap<Integer, ArrayList<Order>> orderHashMap =  new HashMap<Integer, ArrayList<Order>>();

		readData(scaleFactor, customerQTree, customerHashMap, orderBPTree, orderHashMap);

		System.gc();

		System.out.println("Starting Execution");

		int k = 10;
		//int k = 100000;
		//int k = 131072;
		//int k = 65536;
		int countThreshold = 10;
		double countSelectivity = getCountSelectivity(countThreshold, orderHashMap, customerHashMap);

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(path + "Price Selectivity.csv"));
			out.write("Scale factor, k, Price selectivity, Count selectivity, Relational First, kNN First, Optimized\r\n");

			out.flush();

			for (int priceThreshold = 1000; priceThreshold <= 100000; priceThreshold+=5000) {
				SortedMap<Double, ArrayList<Order>> sm = orderBPTree.subMap(orderBPTree.firstKey(), (double)priceThreshold);
				double priceSelectivity = 1 - ((double)sm.size() / orderBPTree.size());

				System.out.println(scaleFactor + ", " + k + "," + priceSelectivity + "," + countSelectivity);
				System.out.println("priceThreshold = " + priceThreshold + ", countThreshold " + countThreshold);

				out.write(scaleFactor + ", " + k + "," + priceSelectivity + "," + countSelectivity + ",");
				run(out, k, priceThreshold, countThreshold, qLocation, customerQTree, customerHashMap, orderBPTree, orderHashMap, priceSelectivity, countSelectivity);
			}

			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void runAllCountSelectivity() {

		int scaleFactor = 1;

		QuadTree customerQTree = new QuadTree(bounds);
		HashMap<Integer, Customer> customerHashMap = new HashMap<Integer, Customer>();

		BPTree<Double, ArrayList<Order>> orderBPTree = new BPTree<Double, ArrayList<Order>>();		
		HashMap<Integer, ArrayList<Order>> orderHashMap =  new HashMap<Integer, ArrayList<Order>>();

		readData(scaleFactor, customerQTree, customerHashMap, orderBPTree, orderHashMap);

		System.gc();

		System.out.println("Starting Execution");

		//int k = 65536;
		int k = 128;
		int priceThreshold = 100000;
		SortedMap<Double, ArrayList<Order>> sm = orderBPTree.subMap(orderBPTree.firstKey(), (double)priceThreshold);
		double priceSelectivity = 1 - ((double)sm.size() / orderBPTree.size());
		System.out.println("priceSelectivity = " + priceSelectivity);

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(path + "Count Selectivity.csv"));
			out.write("Scale factor, k, Price selectivity, Count selectivity, Relational First, kNN First, Optimized\r\n");

			out.flush();

			for (int countThreshold = 0; countThreshold <= 40; countThreshold+=1) {
				double countSelectivity = getCountSelectivity(countThreshold, orderHashMap, customerHashMap);

				System.out.println(scaleFactor + ", " + k + "," + priceSelectivity + "," + countSelectivity);
				System.out.println("priceThreshold = " + priceThreshold + ", countThreshold " + countThreshold);

				out.write(scaleFactor + ", " + k + "," + priceSelectivity + "," + countSelectivity + ",");
				run(out, k, priceThreshold, countThreshold, qLocation, customerQTree, customerHashMap, orderBPTree, orderHashMap, priceSelectivity, countSelectivity);
			}

			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void run(BufferedWriter out, int k, double priceThreshold, int countThreshold, Tuple.Location qLocation, QuadTree customerQTree, HashMap<Integer, Customer> customerHashMap,
			BPTree<Double, ArrayList<Order>> orderBPTree, HashMap<Integer, ArrayList<Order>> orderHashMap, double priceSelectivity, double countSelectivity) throws IOException{

		int repeat = 10;

		kNNToken token;
		PLevelSelect pLevel = new PLevelSelect();

		System.out.println("relational first");
		long startTime = System.nanoTime();
		for (int i = 0; i < repeat; i++)
			token = pLevel.relationalFirst(k, priceThreshold, countThreshold, qLocation, orderBPTree, customerHashMap, customerQTree);
		long endTime = System.nanoTime();

		//		while (token.size() > 0) {
		//			System.out.print(token.remove().distance + " - ");
		//		}
		//		System.out.println();

		double execTime = (endTime - startTime) / 1000000000.0 / repeat;
		out.write(execTime + ", ");
		System.out.println("Execution Time " + execTime);


		System.out.println();
		System.out.println("kNN first");		
		startTime = System.nanoTime();
		for (int i = 0; i < repeat; i++)
			token = pLevel.kNNFirst(k, priceThreshold, countThreshold, qLocation, customerQTree, orderHashMap);
		endTime = System.nanoTime();

		//		while (token.size() > 0) {
		//			System.out.print(token.remove().distance + " - ");
		//		}
		//		System.out.println();

		execTime = (endTime - startTime) / 1000000000.0/ repeat;
		out.write(execTime + ", ");
		System.out.println("Execution Time " + execTime);



		System.out.println();
		System.out.println("OPTIMIZED");		
		startTime = System.nanoTime();
		for (int i = 0; i < repeat; i++)
			token = pLevel.optimized(k, priceThreshold, countThreshold, qLocation, customerQTree, customerHashMap, orderBPTree, orderHashMap, priceSelectivity, countSelectivity);
		endTime = System.nanoTime();

		//		while (token.size() > 0) {
		//			System.out.print(token.remove().distance + " - ");
		//		}
		//		System.out.println();

		execTime = (endTime - startTime) / 1000000000.0/ repeat;
		out.write(execTime + ", ");
		System.out.println("Execution Time " + execTime);




		System.out.println();
		out.write("\r\n");
		out.flush();

		// Check for update frequency in real optimizr (distinct value as an example)
	}

	private static double getCountSelectivity(int countThreshold, HashMap<Integer, ArrayList<Order>> orderHashMap, HashMap<Integer, Customer> customerHashMap) {
		int count = 0;
		for (ArrayList<Order> list : orderHashMap.values()) {
			if (list.size() > countThreshold)
				count++;
		}

		return 1 - ((double)count / customerHashMap.size());
	}

}