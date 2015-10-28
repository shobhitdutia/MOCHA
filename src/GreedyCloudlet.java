import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class GreedyCloudlet {
	ArrayList<IpAddress> ipAddressList;
	ArrayList<IpAddress> currentResponseTime;
	LinkedHashMap<IpAddress, List<Rect>> sendMap;
	
	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}
	public GreedyCloudlet() {
		ipAddressList = new ArrayList<IpAddress>();
		ipAddressList.add(new IpAddress("localhost", 8424, 1));
		ipAddressList.add(new IpAddress("localhost", 8425, 3));
		ipAddressList.add(new IpAddress("localhost", 8426, 7));
		ipAddressList.add(new IpAddress("localhost", 8427, 10));
		Collections.sort(ipAddressList);
		
		sendMap=new LinkedHashMap<GreedyCloudlet.IpAddress, List<Rect>>();
		currentResponseTime=copyList(ipAddressList);		//Initially kept the same.except response time =0
		//currentResponseTime=new ArrayList<GreedyCloudlet.IpAddress>();
	}

	private ArrayList<IpAddress> copyList(ArrayList<IpAddress> ipAddressList2) {
		currentResponseTime=new ArrayList<IpAddress>();
		for(IpAddress ipAddress:ipAddressList) {
			currentResponseTime.add(new IpAddress(ipAddress.hostName, ipAddress.port, 0));
		}
		return currentResponseTime;
	}

	public static void main(String[] args) {
		GreedyCloudlet cloudlet = new GreedyCloudlet();
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(8423);
		} catch (IOException e) {
			e.printStackTrace();
		}
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
	/* Read image from socket. 
	 * Detects faces in an image and passes face to communication thread to send it to the server. 
	 */
	class ProcessImage implements Runnable {
		Socket clientSocket;

		public ProcessImage(Socket clientSocket) {
			this.clientSocket = clientSocket;
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
	        imageMatrix = Imgcodecs.imread("C:\\Users\\shobhitdutia\\Google Drive\\workspace_gdrive\\OpenCVFaceRecog\\bin\\testImage\\group.jpg");
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
				System.out.println("Detected more than 1 face");
				//Assuming faces are less than or equal to number of servers.
				int faceCounter=0;
				List<CommThread> threadList=new ArrayList<CommThread>();
				int processingTime=2;
				
				//Add the first face to the sendMap, since the first value of response time is the minimum response time.
				addToSendMap(ipAddressList.get(0), facesArray[0]);				
				currentResponseTime.get(0).responseTime+=(processingTime+ipAddressList.get(0).responseTime);
/*				System.out.println("Adding 0th face to ip with currentResponseTime "+
						currentResponseTime.get(0).responseTime+" j=0");*/
				System.out.println("Adding 0th face on j=0");
				System.out.println("CurrentResponseTime after 0"+currentResponseTime);
				for(int i=1;i<facesArray.length;i++) {
					int smallest=Integer.MAX_VALUE;
					IpAddress ipAddress;
					int totalProcessTimeForSmallestFace=0;
					int smallestJ=0;
					for(int j=0; j<=i; j++) {
						int rt=ipAddressList.get(j).responseTime;		//Response time of jth server
						int processTimeForFace=rt+processingTime;		//Process time on jth server=above respone time + process time of face on server.
						int latency=processTimeForFace+currentResponseTime.get(j).responseTime;	//Total latency if the above process time is used on jth server
						if(latency<smallest) {
							ipAddress=ipAddressList.get(j);
							smallest=latency;
							smallestJ=j;
							totalProcessTimeForSmallestFace=processTimeForFace;
						}
					}
					currentResponseTime.get(smallestJ).responseTime+=totalProcessTimeForSmallestFace;
					System.out.println("CurrentResponseTime after i="+i+"="+currentResponseTime);
/*					System.out.println("Adding "+i+"th face to ip with currentResponseTime "+
							currentResponseTime.get(i).responseTime+" j="+smallestJ);*/
					System.out.println("Adding "+i+"th face on j="+smallestJ);
					addToSendMap(ipAddressList.get(smallestJ), facesArray[i]);
				}
				System.out.println(sendMap);
				/*for (Map.Entry<Integer, IpAddress> entry : ipAddressList.entrySet()) {
					if(faceCounter==facesArray.length) {
						break;
					}
					Rect faceDimention=facesArray[faceCounter];
					Mat face=gray.submat(faceDimention);
					CommThread commThread=new CommThread(entry.getValue(), face, faceDimention);
					System.out.println("Requesting server "+entry.getKey()+"on host,port:"+entry.getValue().hostName+
							","+entry.getValue().port+" to recognize face"+faceCounter);
					//Thread t=new Thread(commThread);
					threadList.add(commThread);		//Adding to threadlist since i have to wait for threads to complete later.
					commThread.start();
					++faceCounter;
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
		            Imgcodecs.imwrite("frame.png", imageMatrix);	
		            
	                long endTime=System.currentTimeMillis();
	                long time=endTime-startTime;
	                System.out.println("Time taken:"+time);
				 }*/
			}
            //Return image with the recognized face.
		}

		private void addToSendMap(IpAddress ipAddress, Rect rect) {
			List<Rect> faceList;
			if(!sendMap.containsKey(ipAddress)) {		//If no key,
				faceList=new ArrayList<Rect>();			//Create a list
				sendMap.put(ipAddress, faceList);		//Add key value pair to the map.
			} else {									//Otherwise
				faceList=sendMap.get(ipAddress);		//Retrieve the list
			}
			faceList.add(rect);							//Add the value to the list
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

		public CommThread(IpAddress ipAddress, Mat face, Rect faceDim) {
			this.ipAddress=ipAddress;
			this.face=face;
			this.faceDim=faceDim;
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
				System.out.println("Returned condidence:"+confidence);
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
		int port, responseTime;
/*\*/
		public IpAddress(String hostName, int port, int responseTime) {
			this.hostName=hostName;
			this.port=port;
			this.responseTime=responseTime;
		}
		public int compareTo(IpAddress ipAddress) {
			return Integer.compare(this.responseTime, ipAddress.responseTime);
		}
		public String toString() {
			return hostName+";"+port+";"+responseTime;
		}
	}
}
