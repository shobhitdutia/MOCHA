import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.face.Face;
import org.opencv.face.FaceRecognizer;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

public class RecognizeFace {
	FaceRecognizer fr;
	Size trainSize;
	public RecognizeFace() {
		fr = Face.createLBPHFaceRecognizer();
		String personsDir = "C:\\Users\\shobhitdutia\\Google Drive\\workspace_gdrive\\OpenCVFaceRecog\\bin\\pictures";
		trainSize = loadTrainDir(personsDir);
		System.out.println("facerec trained: " + (trainSize != null) + " !");
	}

	public float recognize(Mat face) {
		if (face.size() != trainSize) {
			// not needed for lbph, but for eigen and fisher.
			Imgproc.resize(face, face, trainSize);
		}
		float score = predict(face);
		System.out.println("Score is "+score);
		return score;
	}

	public Size loadTrainDir(String personsDir) {
		Size s = null;
		int label = 0;
		List<Mat> images = new ArrayList<Mat>();
		List<java.lang.Integer> labels = new ArrayList<java.lang.Integer>();
		File node = new File(personsDir);
		String[] subNode = node.list();
		if (subNode == null)
			return null;
		for (String person : subNode) {
			File subDir = new File(node, person);
			if (!subDir.isDirectory())
				continue;
			File[] pics = subDir.listFiles();
			for (File f : pics) {
				Mat m = Imgcodecs.imread(f.getAbsolutePath(), 0);
				if (!m.empty()) {
					images.add(m);
					labels.add(label);
					fr.setLabelInfo(label, subDir.getName());
					s = m.size();
				}
			}
			label++;
		}
		System.out.println("LABELS:" + labels);
		fr.train(images, Converters.vector_int_to_Mat(labels));
		return s;
	}

	public float predict(Mat img) {
		int[] id = { -1 };
		double[] dist = { 0 };
		fr.predict(img, id, dist);
		for (int i = 0; i < id.length; i++) {
			System.out.println("id[" + i + "]" + "=" + id[i]);
		}
		for (int i = 0; i < dist.length; i++) {
			System.out.println("dist[" + i + "]" + "=" + dist[i]);
		}
		if (id[0] == -1)
			return -1;
		float d = ((int) (dist[0] * 100));
		return d / 100;
	}
}
