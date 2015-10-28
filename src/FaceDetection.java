import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.face.Face;
import org.opencv.face.FaceRecognizer;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.utils.Converters;
/*
 * Detects and returns an array of faces from an image.
 */
public class FaceDetection {
	static FaceRecognizer fr = Face.createLBPHFaceRecognizer();
	static CascadeClassifier cascade;
	static Size trainSize;
	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		String cascadeFile = "C:\\Users\\shobhitdutia\\Google Drive\\workspace_gdrive\\OpenCVFaceRecog\\bin\\haarcascade_frontalface_alt.xml";
		cascade = new CascadeClassifier(cascadeFile);
		System.out.println("cascade loaded: " + (!cascade.empty()) + " !");
	}

	public Rect[] getFaces(Mat imageMatrix) {
		Rect[] facesArray = null;
		Mat gray = new Mat();
		Imgproc.cvtColor(imageMatrix, gray, Imgproc.COLOR_BGR2GRAY);
		if (cascade != null) {
			MatOfRect faces = new MatOfRect();
			cascade.detectMultiScale(gray, faces);
			facesArray = faces.toArray();
		}
		return facesArray;
	}
}