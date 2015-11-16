import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
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

public class Cloudlet {
	Map<Integer, IpAddress> responseTime;
	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}
	public Cloudlet() {
		responseTime = new TreeMap<Integer, IpAddress>();
		responseTime.put(2, new IpAddress("52.26.7.241", 8427));
/*		responseTime.put(1, new IpAddress("localhost", 8425));
		responseTime.put(4, new IpAddress("localhost", 8426));
		responseTime.put(3, new IpAddress("localhost", 8427));
		responseTime.put(5, new IpAddress("localhost", 8428));*/
		//responseTime.put(6, new IpAddress("localhost", 8429));
	}

	public static void main(String[] args) {
		Cloudlet cloudlet = new Cloudlet();
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
	        imageMatrix = Imgcodecs.imread("C:\\Users\\shobhitdutia\\Google Drive\\workspace_gdrive\\OpenCVFaceRecog\\bin\\testImage\\1.jpg");
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
				for (Map.Entry<Integer, IpAddress> entry : responseTime.entrySet()) {
					if(faceCounter==1) {
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
				 }
			}
            //Return image with the recognized face.
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
	class IpAddress {
		String hostName;
		int port;
		public IpAddress(String hostName, int port) {
			this.hostName=hostName;
			this.port=port;
		}
	}
}
