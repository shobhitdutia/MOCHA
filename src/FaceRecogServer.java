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
	RecognizeFace recognizeFace; 
	static int averageProcessingTime=0; 
	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}
	public FaceRecogServer() {
		recognizeFace=new RecognizeFace();
	}
	public static void main(String[] args) {
		
		FaceRecogServer faceRecogServer = new FaceRecogServer();

		//Respond to ping from cloudlet.
		LatencyThread latencyThread=faceRecogServer.new LatencyThread();
		new Thread(latencyThread).start();
		
		//Start code for listening incoming face recognition requests from clients. 
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(PORT);
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Listen for incoming clients.
		while (true) {
			Socket clientSocket = null;
			try {
				System.out.println("Server listening for incoming connections on "+PORT);
				clientSocket = serverSocket.accept();
				System.out.println("New request to server");
			} catch (IOException e) {
				e.printStackTrace();
			}
			// Create a new ProcessImage object and start processing image in a
			// new thread.
			RecognizeThread processImage = faceRecogServer.new RecognizeThread(
					clientSocket);
			new Thread(processImage).start();
		}
	}

	/*
	 * Recognize face & return confidence.
	 */
	class RecognizeThread implements Runnable {
		Socket clientSocket;

		public RecognizeThread(Socket clientSocket) {
			this.clientSocket = clientSocket;
		}

		public void run() {
			long faceRecognitionStartTime=System.currentTimeMillis();
			System.out.println("Recognition thread started");
			ObjectInputStream ois;
			byte[]faceBytes = null;
			int rows = 0, columns = 0, type = 0;
			try {
				ois = new ObjectInputStream(clientSocket.getInputStream());
				rows=ois.readInt();
				columns=ois.readInt();
				type=ois.readInt();
				faceBytes= (byte[]) ois.readObject();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			System.out.println("Received byte data of size" +faceBytes.length+" rows:"+rows+";columns="+columns+";type="+type);
			Mat incomingFace=new Mat(rows,columns,type);
			incomingFace.put(0, 0, faceBytes);
			
			System.out.println("Recognizing face");
			Imgcodecs.imwrite("face"+faceBytes.length+".png", incomingFace);

			float confidence=recognizeFace.recognize(incomingFace);
			long faceRecognitionEndTime=System.currentTimeMillis();
			averageProcessingTime=(int) (faceRecognitionEndTime-faceRecognitionStartTime);
			System.out.println("Recognition time"+averageProcessingTime);
			System.out.println("Returning confidence"+confidence);
			ObjectOutputStream oos;
			try {
				oos = new ObjectOutputStream(clientSocket.getOutputStream());
				oos.writeObject(confidence);
				System.out.println("Server job for face completed!");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

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
					oos.writeInt(averageProcessingTime);
					oos.flush();
					System.out.println("averageProcessingTime returned"+averageProcessingTime);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
