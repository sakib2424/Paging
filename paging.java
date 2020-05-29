import java.util.*;
import java.io.*;

public class paging {
	private static Scanner randomIntScanner;	
	static ArrayList<Process> processList;
	static ArrayList<Frame> frameList;
	static int M;	//  The machine size in words
	
	static int P;	//	The page size in words
	static int S;	//	The size of each process
	static int N;	//	The number of references for each process
	
	static int J;	//	The “job mix”
	static String R; //	The replacement algorithm, FIFO, RANDOM, or LRU	
	
	public static void main(String args[]) throws FileNotFoundException {
		if (args.length != 6) {
			System.out.println("Invalid Output");
			System.exit(1);
		}
//		This scanner will be used throughout the program for random numbers	
		randomIntScanner = new Scanner(new File("random-numbers"));
//		Calls a method to process our inputs
		processInput(args);
//		Runs the simulation
		runSimulation();
		
		randomIntScanner.close();
//		Prints the output
		generateOutput();
	}
	
	static void runSimulation() {
		int time = 1;
		int processesCompleted = 0;
//		Run this loop until the all processes complete 
		while (processesCompleted < processList.size()) {
//			Iterate over the process array
			for (Process process: processList) {
//				Give each process a quanta of three 
				for (int i = 0; i < 3; i++) {	
//					System.out.print("\nTime: " + time + " Process " + (process.index+1) + " references " + process.currentReference);
					
//					Do nothing if process is completed
					if (process.isFinished()) {
						break;
					}
//					If the process has reached its reference goal, complete it 
					if (process.completeIfPossible()) {
						processesCompleted++;
						break;
					}
					
					int currentPage = process.getCurrentPage();
//					See if the reference can be granted immediately
					Frame foundFrame = checkAvailability(process);
//					If it exists, then simply update the last use time
					if (foundFrame != null) {
//						System.out.print(" hit in frame " + frameList.indexOf(foundFrame));
						foundFrame.updateLastUse(time);
					}
					else {
//						The frame is not there so we increment fault count
						process.incrementFaultCount();
//						Check if there is an empty spot available 
						int emptySpot = findEmptySpot();
//						If there is an empty slot, populate it 
						if (emptySpot != -1) {
//							System.out.print(" using free frame " + emptySpot);
							frameList.get(emptySpot).switchFrames(time, process.index, currentPage);
						}
//						If there is no spot available, remove an existing frame 
						else {
//							Select frame to delete
							Frame selectedForDeletion = findFrameToRemove();
							Process associatedProcess = processList.get(selectedForDeletion.processIndex);
														
//							System.out.print(" evicting page " + selectedForDeletion.pageNumber + 
//									" of " + (associatedProcess.index+1) + " from frame " + 
//									frameList.indexOf(selectedForDeletion));
							
//							Set numbers for eviction 
							associatedProcess.evict(time - selectedForDeletion.firstReferenceTime);
							
//							Now replace the frame
							selectedForDeletion.switchFrames(time, process.index, currentPage);
						}
						
						
					}
//					Update the processe's next reference 
					process.setNextReference(randomIntScanner);
//					The process has successfully completed a reference 
					process.referenceCount++;
//					Increment time 
					time++;
					
				}
//				System.out.println();
			}
			
			
			
		}
		
	}
	
//	Returns an empty spot if it exists, else returns -1 
	static int findEmptySpot() {
		for (int i = frameList.size() - 1; i >= 0; i--) {
			if (!frameList.get(i).beingUsed) {
				return i;
			}
		}
		return -1;
	}
//	Calls the proper algorithm to replace 
	static Frame findFrameToRemove() {
		Frame toReturn = null;
		switch (R) {
		case ("lru"):
			toReturn = LRU();
			break;
		case("fifo"):
			toReturn = FIFO();
			break;
		case("random"):
			toReturn = random();
			break;
		}
		return toReturn;
	}
//	Random algorithm 
	static Frame random() {
		int randomNumber = randomIntScanner.nextInt();
		int index = randomNumber % frameList.size();
		return frameList.get(index);
	}
//	First comes first out algorithm 
	static Frame FIFO() {
		Frame toReturn = null;
		int currentOldestFrameTime = Integer.MAX_VALUE;
		for (Frame frame: frameList) {
			if (frame.firstReferenceTime < currentOldestFrameTime) {
				toReturn = frame;
				currentOldestFrameTime = frame.firstReferenceTime;
			}
		}
		return toReturn;
	}
//	Least recently used algorithm 
	static Frame LRU() {
		Frame toReturn = null;
		int currentSmallestValue = Integer.MAX_VALUE;
		for (Frame frame: frameList) {
			if (frame.lastReference < currentSmallestValue) {
				toReturn = frame;
				currentSmallestValue = frame.lastReference;
			}
		}
		return toReturn;
	}
//	Checks if reference can be done with no fault 
	static Frame checkAvailability (Process process) {
		for (Frame frame : frameList) {
			if (process.index == frame.processIndex && frame.beingUsed &&
					process.getCurrentPage() == frame.pageNumber) {
				return frame;
			}
		}
		return null;
	}
//	Method to process the input and popualte arrays 
	static void processInput(String[] args) {
		processList = new ArrayList<>();
		frameList = new ArrayList<>();
		
		M = Integer.parseInt(args[0]);
		P = Integer.parseInt(args[1]);
		S = Integer.parseInt(args[2]);
		J = Integer.parseInt(args[3]);
		N = Integer.parseInt(args[4]);
		R = args[5];
		
		Process.setStatics(P, S, N);
		
		switch (J) {
		case 1:
			processList.add(new Process(1.0, 0.0, 0.0, 0));
			break;
		case 2:
			for (int i = 0; i < 4; i++) {
				processList.add(new Process(1.0, 0.0, 0.0, i));
			}
			break;
		case 3:
			for (int i = 0; i < 4; i++) {
				processList.add(new Process(0.0, 0.0, 0.0, i));
			}
			break;
		case 4:
			processList.add(new Process(0.75, 0.250, 0.000, 0));
			processList.add(new Process(0.75, 0.000, 0.250, 1));
			processList.add(new Process(0.75, 0.125, 0.125, 2));
			processList.add(new Process(0.50, 0.125, 0.125, 3));
			break;
		}
		
		int totalNumberOfFrames = M / P;
		for (int i=0; i < totalNumberOfFrames; i++) {
			frameList.add(new Frame()); 
		}
	}
//	Prints the final output to the console 
	static void generateOutput() {
		System.out.printf("The machine size is %d.\nThe page size is %d.\n"
				+ "The process size is %d.\nThe job mix is %d\n"
				+ "The number of references per process is %d\n"
				+ "The replacement algorithm is %s\n"
				+ "The level of debugging output is 0\n", M, P, S, J, N, R);
		int totalFaults = 0;
		int totalResidency = 0;
		int totalEvictions = 0;
		
		for (Process process: processList) {
			totalFaults += process.faultCount;
			totalResidency += process.totalResidencyTime;
			totalEvictions += process.evictionsCount;
			System.out.printf("\nProcess %d had %d faults", 
					process.index + 1, process.faultCount);
			if (process.evictionsCount == 0) {
				System.out.println("\nWith no evictions, the average "
						+ "residence is undefined");
			}
			else {
				double averageResidency = process.totalResidencyTime / 
						(double)process.evictionsCount;
				System.out.printf(" and %f average residency\n", averageResidency);
			}
		}
		
		if (totalResidency != 0) {
			double totalAverageResidency = (double) totalResidency / totalEvictions;
			
			System.out.printf("\nThe total number of faults is %d , the "
					+ "overall average residency is %f", totalFaults, totalAverageResidency);
		}
		else {
			System.out.printf("\nThe total number of faults is %d ,"
					+ "the overall average residency is undefined", totalFaults);
		}
	}

}

//A class to represent a process 
class Process {
	static int P;
	static int S;	
	static int N;
	
	double A,B,C; //	Probabilities
	int index; //	Used to uniquely identify the process
	int currentReference;
	int referenceCount = 0;
	int faultCount = 0;
	int evictionsCount = 0;
	int totalResidencyTime = 0;
	public boolean completed = false;
//	Constructor 
	public Process(double d, double e, double f, int index) {
		this.A = d;
		this.B = e;
		this.C = f;
		this.index = index;
		this.currentReference = (111 * (index + 1)) % S;
	}
//	Handles eviction duties 
	public void evict(int timeToAppend) {
		this.evictionsCount++;
		this.totalResidencyTime += timeToAppend;
	}
//	Sets global variables 
	public static void setStatics (int pageSize, 
			int processSize, int numberOfReferences) {
		P = pageSize;
		S = processSize;
		N = numberOfReferences;
	}
	
	public boolean isFinished() {
		return completed;
	}
	
	public void incrementFaultCount() {
		faultCount++;
	}
	
	public boolean completeIfPossible() {
		if (referenceCount != N) {
			return false;
		}
		else {
			this.completed = true;
			return true;
		}
	}
	
	public int getCurrentPage() {
		return currentReference / P;
	}
//	Creates the next reference of the process 
	public void setNextReference (Scanner scanner) {
		double number = scanner.nextInt() / (Integer.MAX_VALUE + 1d);
		int newReference = -1;
		
		if (number < this.A){
			newReference = (this.currentReference + 1) % S;
		}
		else if (number < this.A + this.B){
			newReference = (this.currentReference + S - 5) % S;
		}
		else if (number < this.A + this.B + this.C){
			newReference = (this.currentReference + 4) % S;
		}
		else{
			int temp = scanner.nextInt();
			newReference = temp % S;
		}
		this.currentReference = newReference;
	}
	
}

//	An object to represent frames 
class Frame {
	public int processIndex;	// Index of the process which is using this frame
	public int pageNumber;		// Page number of current reference of the process using this frame
	
	public int firstReferenceTime;	// When this frame was first referenced by the current process
	public int lastReference;		// Last time this frame was accessed by the current process
	
	public boolean beingUsed; 	// Indicates whether or not the frame is being referenced
	
	public Frame() {
		this.beingUsed = false;
	}
	
	public void updateLastUse(int time) {
		this.lastReference = time;
	}
//	Switches the frame to the next one 
	public void switchFrames(int currentTime, int newIndex, int newPageNumber) {
		this.beingUsed = true;
		this.firstReferenceTime = currentTime;
		this.lastReference = currentTime;
		this.pageNumber = newPageNumber;
		this.processIndex = newIndex;
	}
//	Prints the frame object 
	public String toString() {
		String output = String.format("Process: %d Page : %d FirstRef: %d", 
				this.processIndex+1, this.pageNumber, this.firstReferenceTime);
		return output;
	}
}










