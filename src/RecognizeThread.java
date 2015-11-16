import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

/*
	 * Recognize face & return confidence.
	 */
	class RecognizeThread extends Thread {
		Socket clientSocket;
		RecognizeFace recognizeFace; 
		static int averageProcessingTime;
		
		public RecognizeThread(Socket clientSocket) {
			this.clientSocket = clientSocket;
			recognizeFace=new RecognizeFace();
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