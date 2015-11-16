import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class GreedyCloudlet {
	List<IpAddress> ipAddressList;
	Map<String, IpAddress> currentResponseTimeMap;
	
	
	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}
	public GreedyCloudlet() {
		ipAddressList = Collections.synchronizedList(new ArrayList<IpAddress>());
		currentResponseTimeMap=new ConcurrentHashMap<String, IpAddress>();
		ipAddressList.add(new IpAddress("52.33.213.15", 8427, 8428, 1, 2));	//mocha1
		currentResponseTimeMap.put("52.33.213.15",new IpAddress("52.33.213.15", 8427, 8428, 0, 0));

		/*ipAddressList.add(new IpAddress("52.33.242.234", 8427, 8428, 3, 2));	//mocha 1.1
		currentResponseTimeMap.put("52.33.242.234",new IpAddress("52.33.242.234", 8427, 8428, 0, 0));
		
		ipAddressList.add(new IpAddress("52.26.77.103", 8427, 8428, 7, 2));	  //mocha 1.2
		currentResponseTimeMap.put("52.26.77.103",new IpAddress("52.26.77.103", 8427, 8428, 0, 0));
		
		ipAddressList.add(new IpAddress("52.33.160.214", 8427, 8428, 10, 2));  //mocha 1.3
		currentResponseTimeMap.put("52.33.160.214",new IpAddress("52.33.160.214", 8427, 8428, 0, 0));*/
		
		ipAddressList.add(new IpAddress("52.26.7.241", 8427, 8428, 15, 2));		//mocha 1.4
		currentResponseTimeMap.put("52.26.7.241",new IpAddress("52.26.7.241", 8427, 8428, 0, 0));
	
		
 /* 	ipAddressList.add(new IpAddress("localhost", 8427, 8428, 0));
		ipAddressList.add(new IpAddress("localhost", 8427, 8428, 0));
		ipAddressList.add(new IpAddress("localhost", 8427, 8428, 0));*/
		
		Collections.sort(ipAddressList);
		System.out.println(ipAddressList);
	}

	public static void main(String[] args) throws InterruptedException {
		GreedyCloudlet cloudlet = new GreedyCloudlet();
		//cloudlet.getServerLatencies();
		//Keep  updating server latencies, every 5 seconds.
		LatencyThread latencyThread=cloudlet.new LatencyThread();
		new Thread(latencyThread).start();
		Thread.sleep(10000);
/*		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(8423);
		} catch (IOException e) {
			e.printStackTrace();
		}*/
		//Create a new ProcessImage object and start processing image in a new thread.
		//COMMENT THIS LATER
		ProcessImage processImage = cloudlet.new ProcessImage(null);
		new Thread(processImage).start();
/*		// Listen for incoming clients.
		//UNCOMMENT THIS LATER!!!!!!!!!!
		while (true) {
			Socket clientSocket = null;
			try {
				clientSocket = serverSocket.accept();
			} catch (IOException e) {
				e.printStackTrace();
			}
			//Create a new ProcessImage object and start processing image in a new thread.
			ProcessImage processImage = cloudlet.new ProcessImage(clientSocket);
			new Thread(processImage).start();
		}*/
	}
	/*
	 * Get the server latencies & processing times. 
	 * Loops over all the servers and updates the servers' processing latency and rtt.
	 */
	private void getServerLatencies() {
		Socket clientLatencySocket = null;
		try {
			//Loop over all servers and get latencies
			for(IpAddress ipAddress:ipAddressList) {
				System.out.println("Getting latency from ip"+ipAddress.hostName);
				
				clientLatencySocket=new Socket(ipAddress.hostName, ipAddress.latencyPort);
				long latencyStartTime=System.currentTimeMillis();
				ObjectOutputStream oos=new ObjectOutputStream(clientLatencySocket.getOutputStream());
				
				oos.writeObject("getLatency");
				ObjectInputStream ois=new ObjectInputStream(clientLatencySocket.getInputStream());
				int recognitionTime=ois.readInt();
				long latencyEndTime=System.currentTimeMillis();
				
				int totalLatencyToServer=(int) (latencyEndTime-latencyStartTime);
				ipAddress.processingTime=recognitionTime;
				ipAddress.responseTime=totalLatencyToServer;
				System.out.println("Latency time:"+totalLatencyToServer+", Recognition time:"+recognitionTime);
				
			}
			System.out.println();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/* Read image from socket. 
	 * Detects faces in an image and passes face to communication thread to send it to the server. 
	 */
	class ProcessImage implements Runnable {
		Socket clientSocket;
		LinkedHashMap<IpAddress, List<ProcessingTimeAndFace>> sendMap;	
		//To keep track of how much response time is added to a particular Ip address in 
		//currentResponseTimeMap. This is done so that we can subtract this time after server
		//has processed that image. 
		
		public ProcessImage(Socket clientSocket) {
			this.clientSocket = clientSocket;
			//Node, sendMap is local to each thread. 
			this.sendMap=new LinkedHashMap<IpAddress, List<ProcessingTimeAndFace>>();
		}

		public void run() {
			ObjectInputStream objectInputStream;
			Mat imageMatrix = null;
/*			try {
				objectInputStream = new ObjectInputStream(
						clientSocket.getInputStream());
				imageMatrix = (Mat) objectInputStream.readObject();
				System.out.println("Received incoming image");
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}*/
	        imageMatrix = Imgcodecs.imread("C:\\Users\\shobhitdutia\\Google Drive\\workspace_gdrive\\OpenCVFaceRecog\\bin\\testImage\\s310.pgm");
			FaceDetection faceDetection = new FaceDetection();
			System.out.println("Detecting faces in image");
			
			long startTime=System.currentTimeMillis();
			Rect[] facesArray = faceDetection.getFaces(imageMatrix);
			long medTime=System.currentTimeMillis();
			long detectionTime=medTime-startTime;
			System.out.println("Face detection time="+detectionTime);
			System.out.println();
			System.out.println("Detected"+facesArray.length+" faces");
	        //Convert original image to gray for use in face recognition.
			Mat gray = new Mat();	
	        Imgproc.cvtColor(imageMatrix, gray, Imgproc.COLOR_BGR2GRAY);
			if(facesArray.length>0) {
				//Make a copy of the ipAddressList so that if ip's change, it is not reflected while iterating the list.
				ArrayList<IpAddress> copyOfIpAddressList=cloneList();
				Collections.sort(copyOfIpAddressList);
				System.out.println("Processing on Copy of list:"+copyOfIpAddressList);
				System.out.println("Detected more than 1 face");
				//Assuming faces are less than or equal to number of servers.
				//int faceCounter=0;
				List<CommThread> threadList=new ArrayList<CommThread>();
				
				
				//Add the first face to the sendMap, since the first value of response time is the minimum response time.
				int rttTobeAdded=copyOfIpAddressList.get(0).processingTime+copyOfIpAddressList.get(0).responseTime;
				addToSendMap(copyOfIpAddressList.get(0), facesArray[0], rttTobeAdded);
				IpAddress ipAddressFromMap=currentResponseTimeMap.get(copyOfIpAddressList.get(0).hostName);	//Get ip of server with least response time
				
				synchronized(ipAddressFromMap) {
					ipAddressFromMap.responseTime+=rttTobeAdded;					
				}
/*				System.out.println("Adding 0th face to ip with currentResponseTime "+
						currentResponseTime.get(0).responseTime+" j=0");*/
				System.out.println("Adding 0th face "+facesArray[0]+" on j=0");
				System.out.println("CurrentResponseTime after 0"+currentResponseTimeMap);
				//Assuming that number of servers is more than number of faces in the array
				for(int i=1;i<facesArray.length;i++) {
					int smallest=Integer.MAX_VALUE, smallestJ=0;
//					String hostNameOfSmallestLatency = null;
					IpAddress smallestIpAddressFromMap = null;
					int totalProcessTimeForSmallestFace=0;
					for(int j=0; j<=i && j< ipAddressList.size(); j++) {
						ipAddressFromMap=currentResponseTimeMap.get(copyOfIpAddressList.get(j).hostName);	//Get ip of server with least response time
						//ipAddressFromMap.responseTime+=(copyOfIpAddressList.get(0).processingTime+copyOfIpAddressList.get(0).responseTime);
						
						int rt=copyOfIpAddressList.get(j).responseTime;		//Response time of jth server
						int processingTime=copyOfIpAddressList.get(j).processingTime;
						int processTimeForFace=rt+processingTime;		//Process time on jth server=above respone time + process time of face on server.
						int latency=processTimeForFace+ipAddressFromMap.responseTime;	//Total latency if the above process time is used on jth server
						if(latency<smallest) {
							//hostNameOfSmallestLatency=copyOfIpAddressList.get(j).hostName;
							smallestIpAddressFromMap=ipAddressFromMap;
							smallest=latency;
							smallestJ=j;
							totalProcessTimeForSmallestFace=processTimeForFace;
						}
					}
					//ipAddressFromMap=currentResponseTimeMap.get(hostNameOfSmallestLatency);
					synchronized(smallestIpAddressFromMap) {
						smallestIpAddressFromMap.responseTime+=totalProcessTimeForSmallestFace;						
					}
					System.out.println("CurrentResponseTime after i="+i+"="+currentResponseTimeMap);
/*					System.out.println("Adding "+i+"th face to ip with currentResponseTime "+
							currentResponseTime.get(i).responseTime+" j="+smallestJ);*/
					System.out.println("Adding "+i+"th face "+facesArray[i]+" on j="+smallestJ);
					addToSendMap(smallestIpAddressFromMap, facesArray[i], totalProcessTimeForSmallestFace);
				}
				System.out.println(sendMap);
				//Code to send detected images to assigned servers
				for (Map.Entry<IpAddress, List<ProcessingTimeAndFace>> entry : sendMap.entrySet()) {
					IpAddress ipAddress=entry.getKey();
					List<ProcessingTimeAndFace> processTimeAndFaceList=entry.getValue();
					for(ProcessingTimeAndFace processingTimeAndFace:processTimeAndFaceList) {
						Rect faceDimention=processingTimeAndFace.rect;
						int processingTimeForFace=processingTimeAndFace.processingTime;
						Mat face=gray.submat(faceDimention);
						CommThread commThread=new CommThread(ipAddress, face, faceDimention, processingTimeForFace);
						System.out.println("Requesting server "+ipAddress+" to recognize face"+ faceDimention);
						//Thread t=new Thread(commThread);
						threadList.add(commThread);		//Adding to threadlist since i have to wait for threads to complete later.
						commThread.start();						
					}

				//	++faceCounter;
				}
				//Wait for all threads to complete
				System.out.println("Wait for all threads to complete");
				for(Thread t:threadList) {
					try {
						t.join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				System.out.println("ALl Threads completed");
				CommThread smallestThresholdThread=null;
				 float smallestScore=Integer.MAX_VALUE;
				 for(CommThread commThread:threadList) {
					 float s = commThread.confidence; 
	                 if(s!=-1&& s<smallestScore) {
	                 	smallestScore=s;
	                 	smallestThresholdThread=commThread;
	                 }
				 }
				 //Check if there was atleast 1 face recognized.
				 if(smallestThresholdThread!=null) {
					 //Now we have the thread with the smallest value of the face.
					 //Get the face.
					 System.out.println("Writing most probable recognized face");
					 //Create a box around the face in the original image and return it to the client.
					Rect face=smallestThresholdThread.faceDim;
		         	Imgproc.rectangle(imageMatrix, face.tl(), face.br(), new Scalar(0,200,0), 3);
		            System.out.println("Writing");
		            Imgproc.putText(imageMatrix, smallestScore+"", 
                    		new Point(40,40), Core.FONT_HERSHEY_PLAIN,1.3,new Scalar(0,0,200),2);
		            Imgcodecs.imwrite("frame.pgm", imageMatrix);	
		            
	                long endTime=System.currentTimeMillis();
	                long time=endTime-startTime;
	                System.out.println("Time taken:"+time);
				 }
			}
            //Return image with the recognized face.
		}

		/*
		 * Returns a copy of ipAddresslist
		 */
		private ArrayList<IpAddress> cloneList() {
			synchronized(ipAddressList) {
				ArrayList<IpAddress> clonedList=new ArrayList<IpAddress>();
				for(IpAddress ipAddress:ipAddressList) {
					clonedList.add(new IpAddress(ipAddress.hostName, ipAddress.port,
							ipAddress.latencyPort, ipAddress.responseTime, ipAddress.processingTime));
				}
				return clonedList;				
			}

		}
	/*
	 * 	Creates the mapping with key:IpAddress and value: ProcessingTimeAndFace object
	 * 	The ProcessingTimeAndFaceObject keeps information of the face(Rect) to be added
	 * and the estimated response time that this face will take to recognize on the server.
	 */
		private void addToSendMap(IpAddress ipAddress, Rect rect, int rttTobeAdded) {
			List<ProcessingTimeAndFace> processingTimeAndFaceList;
			ProcessingTimeAndFace ptf=new ProcessingTimeAndFace(rect, rttTobeAdded);	//Create the required object
			if(!sendMap.containsKey(ipAddress)) {		//If no key,
				processingTimeAndFaceList=new ArrayList<ProcessingTimeAndFace>();  //Create a list
				sendMap.put(ipAddress, processingTimeAndFaceList);		//Add key value pair to the map.
			} else {									//Otherwise
				processingTimeAndFaceList=sendMap.get(ipAddress);		//Retrieve the list
				System.out.println("LIST Retrieved");
			}
			processingTimeAndFaceList.add(ptf);							//Add the value to the list
		}
		
		class ProcessingTimeAndFace {
			Rect rect;
			int processingTime;
			
			public ProcessingTimeAndFace(Rect rect, int processingTime) {
				this.rect=rect;
				this.processingTime=processingTime;
			}
			public String toString() {
				return rect.toString()+", "+processingTime;
			}
		}
	}
	/*
	 * Accepts an ip address and sends the face to be recognized to the server.
	 */
	class CommThread extends Thread {
		IpAddress ipAddress;
		Mat face;
		Rect faceDim;
		float confidence;
		int responseTime;

		/*
		 * ipAddress IP address of current host
		 * face The face which is sent to the server
		 * faceDim dimentions of face
		 * responseTime Response time that is added to currentResponseTime table
		 * This responseTime is needed in order to subtract it from currentResponseTime table
		 * when communication is completed
		 */
		public CommThread(IpAddress ipAddress, Mat face, Rect faceDim, 
				int responseTime) {
			this.ipAddress=ipAddress;
			this.face=face;
			this.faceDim=faceDim;
			this.responseTime=responseTime;
		}

		public void run() {
			Socket socket=null;
			try {
				socket=new Socket(ipAddress.hostName, ipAddress.port);
				ObjectOutputStream oos=new ObjectOutputStream(socket.getOutputStream());
				//Get rows, columns so that we can send data as a byte.
				int rows=face.rows();
				int column=face.cols();
				int type=face.type();
				byte[] faceBytes=new byte[column*rows*(int)face.elemSize()];	//Since mat is not serializable
				face.get(0,0,faceBytes);
				oos.writeInt(rows);
				oos.writeInt(column);
				oos.writeInt(type);
				oos.writeObject(faceBytes);
				ObjectInputStream ois=new ObjectInputStream(socket.getInputStream());
				System.out.println("Getting confidence value of face from server");
				confidence=(Float) ois.readObject();
				//subtract processing time from the currentResponseTime table
				IpAddress ipAddressFromCurrentResponseTimeMap=currentResponseTimeMap.get(ipAddress.hostName);
				synchronized (ipAddressFromCurrentResponseTimeMap) {
					ipAddressFromCurrentResponseTimeMap.responseTime-=responseTime;		//Subtracting estimated response time on server
				}
				System.out.println("Returned condidence:"+confidence);
				System.out.println("Subtracting estimated response time "+responseTime+" on server"+ipAddress);
				System.out.println("Updated currentResponseTimeMap: \n"+currentResponseTimeMap);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	class IpAddress implements Comparable<IpAddress>{
		String hostName;
		int port, responseTime, latencyPort, processingTime;
/*\*/
		public IpAddress(String hostName, int port, int latencyPort, 
				int responseTime, int processingTime) {
			this.hostName=hostName;
			this.port=port;
			this.latencyPort=latencyPort;
			this.responseTime=responseTime;
			this.processingTime=processingTime;
		}
		public int compareTo(IpAddress ipAddress) {
			return Integer.compare(this.responseTime, ipAddress.responseTime);
		}
		public String toString() {
			return "IP:"+hostName+", PORT:"+port+", RTT:"+responseTime;
		}
		@Override
		public int hashCode() {
			return hostName.hashCode();
		}
		@Override
		public boolean equals(Object o) {
			return hostName.equals(o);
		}
	}
	
	class LatencyThread implements Runnable {

		public void run() {
				while(true) {
					getServerLatencies();
					Collections.sort(ipAddressList);
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
		}
	}
}
