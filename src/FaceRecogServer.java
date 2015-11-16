import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

public class FaceRecogServer {
	public static final int PORT=8427, LATENCY_PORT=8428;
	static RecognizeFace recognizeFace; 
	
	//static int averageProcessingTime=0; 
	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}
	public FaceRecogServer() {

	}
	public static void main(String[] args) {
		
		FaceRecogServer faceRecogServer = new FaceRecogServer();
		recognizeFace=new RecognizeFace();
		//Respond to ping from cloudlet.
		LatencyThread latencyThread=faceRecogServer.new LatencyThread();
		new Thread(latencyThread).start();
		
		//Start code for listening incoming face recognition requests from clients. 
		ServerSocket serverSocket = null;
		Socket clientSocket = null;
		try {
			serverSocket = new ServerSocket(PORT);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Listen for incoming clients.
		while (true) {

			try {
				System.out.println("Server listening for incoming connections on "+PORT);
				clientSocket = serverSocket.accept();
				System.out.println("New request to server");
			} catch (IOException e) {
				e.printStackTrace();
			}
			// Create a new ProcessImage object and start processing image in a
			// new thread.
/*			RecognizeThread processImage = faceRecogServer.new RecognizeThread(
					clientSocket);*/
			new RecognizeThread(clientSocket).start();
		}
	}

	
	class LatencyThread implements Runnable {

		public void run() {
			ServerSocket serverLatencySocket = null;
			try {
				serverLatencySocket = new ServerSocket(LATENCY_PORT);
			} catch (IOException e) {
				e.printStackTrace();
			}
			// Listen for incoming latency requests from clients.
			while (true) {
				Socket clientLatencySocket = null;
				ObjectInputStream ois=null;
				try {
					System.out.println("Server listening for incoming latency requests on "+InetAddress.getLocalHost().getHostAddress()+LATENCY_PORT);
					clientLatencySocket = serverLatencySocket.accept();

					ois=new ObjectInputStream(clientLatencySocket.getInputStream());
					String requestType=(String) ois.readObject();
					System.out.println("New latency request to server:"+requestType);					
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("Returning average processing time with the latency request");
				ObjectOutputStream oos;
				try {
					oos = new ObjectOutputStream(clientLatencySocket.getOutputStream());
					//oos.writeInt(averageProcessingTime);
					oos.writeInt(RecognizeThread.averageProcessingTime);
					oos.flush();
					System.out.println("averageProcessingTime returned"+RecognizeThread.averageProcessingTime);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
