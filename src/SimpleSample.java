import java.io.File;
import java.util.ArrayList;
import java.util.List;

import lowgui.NamedWindow;

import org.opencv.core.Core;
import org.opencv.core.CvType;
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
import org.opencv.videoio.VideoCapture;



class FaceRec {
    static FaceRecognizer fr = Face.createLBPHFaceRecognizer();
    
    //
    // unlike the c++ demo, let's not mess with csv files, but use a folder on disk.
    //    each person should have its own subdir with images (all images the same size, ofc.)
    //
    public Size loadTrainDir(String dir)
    {
        Size s = null;
        int label = 0;
        List<Mat> images = new ArrayList<Mat>();
        List<java.lang.Integer> labels = new ArrayList<java.lang.Integer>();
        File node = new File(dir);
        String[] subNode = node.list();
        if ( subNode==null ) return null;
        for(String person : subNode) {
            File subDir = new File(node, person);
            if ( ! subDir.isDirectory() ) continue;
            File[] pics = subDir.listFiles();
            for(File f : pics) {
                Mat m = Imgcodecs.imread(f.getAbsolutePath(),0);
                if (! m.empty()) {
                    images.add(m);
                    labels.add(label);
                    fr.setLabelInfo(label,subDir.getName());
                    s = m.size();
                }
            }
            label ++;
        }
        System.out.println("LABELS:"+labels);
        fr.train(images, Converters.vector_int_to_Mat(labels));
        return s;
    }

    public float predict(Mat img)
    {
        int[] id = {-1};
        double[] dist = {0};
        fr.predict(img,id,dist);
        for(int i=0;i<id.length;i++) {
        	System.out.println("id["+i+"]"+"="+id[i]);
        }
        for(int i=0;i<dist.length;i++) {
        	System.out.println("dist["+i+"]"+"="+dist[i]);
        }
        if (id[0] == -1)
            return -1;
        float d = ((int)(dist[0]*100));
        //return fr.getLabelInfo(id[0]) + " : " + d/100;
        return d/100;
    }
}

//
// SimpleSample [persons_dir] [path/to/face_cascade]
//
public class SimpleSample {

    static{ System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    public static void main(String[] args) {
        String personsDir = "C:\\Users\\shobhitdutia\\Google Drive\\workspace_gdrive\\OpenCVFaceRecog\\bin\\pictures";
        if (args.length > 1) personsDir = args[1];

        String cascadeFile = "C:\\Users\\shobhitdutia\\Google Drive\\workspace_gdrive\\OpenCVFaceRecog\\bin\\haarcascade_frontalface_alt.xml";
        if (args.length > 2) cascadeFile = args[2];

        CascadeClassifier cascade = new CascadeClassifier(cascadeFile);
        System.out.println("cascade loaded: "+(!cascade.empty())+" !");

        FaceRec face = new FaceRec();
        Size trainSize = face.loadTrainDir(personsDir);
        System.out.println("facerec trained: "+(trainSize!=null)+" !");

        NamedWindow    frame = new NamedWindow("Face");

        VideoCapture cap = new VideoCapture(0);
        if (! cap.isOpened()) {
            System.out.println("Sorry, we could not open you capture !");
        }
        
        Mat im = Imgcodecs.imread("C:\\Users\\shobhitdutia\\Google Drive\\workspace_gdrive\\OpenCVFaceRecog\\bin\\testImage\\group.jpg");
        long startTime=System.currentTimeMillis();
        Mat gray = new Mat();	
        Imgproc.cvtColor(im, gray, Imgproc.COLOR_BGR2GRAY);
        
        if (cascade != null ) {
            MatOfRect faces = new MatOfRect();
            cascade.detectMultiScale(gray, faces);
            Rect[] facesArray = faces.toArray();
            System.out.println("faces length "+facesArray.length);
            float smallest=Integer.MAX_VALUE;
            Rect smallestFace = null;
            if (facesArray.length != 0) {
            	for(int i=0;i<facesArray.length;i++) {
                    Rect found = facesArray[i];
                    //Imgproc.rectangle(im, found.tl(), found.br(), new Scalar(0,200,0), 3);
                    
                    Mat fi = gray.submat(found);
                    if (fi.size() != trainSize) // not needed for lbph, but for eigen and fisher
                        Imgproc.resize(fi,fi,trainSize);
 
                    //REMOVE THIS
                    int rows=fi.rows();
    				int column=fi.cols();
    				int elemSize=(int) fi.elemSize();
    				int type=fi.type();
    				System.out.println(type);
    			
    				byte[] faceBytes=new byte[column*rows*elemSize];	//Since mat is not serializable
    				fi.get(0, 0,faceBytes);
    				Mat incomingFace=new Mat(rows,column,type);
    				incomingFace.put(0, 0, faceBytes);
                    Imgcodecs.imwrite("face"+i+".png", incomingFace);      				
                    //TILL HERE

                    float s = face.predict(fi); 
                    if(s<smallest) {
                    	smallest=s;
                    	smallestFace=found;
                    }
                    System.out.println("Score: "+s);
                    if (s != -1)		//put score on image
                        Imgproc.putText(im, s+"", new Point(40,40), Core.FONT_HERSHEY_PLAIN,1.3,new Scalar(0,0,200),2);

            	}
            	System.out.println("Smallest is"+smallest);
            	Imgproc.rectangle(im, smallestFace.tl(), smallestFace.br(), new Scalar(0,200,0), 3);
                System.out.println("Writing");
                Imgcodecs.imwrite("frame.png", im);          		            	
                
                long endTime=System.currentTimeMillis();
                long time=endTime-startTime;
                System.out.println("Time taken:"+time);
                //frame.imshow(im);

        	}
        }
/*        while (cap.read(im)) {
            Mat gray = new Mat();
            Imgproc.cvtColor(im, gray, Imgproc.COLOR_BGR2GRAY);
            if (cascade != null ) {
                MatOfRect faces = new MatOfRect();
                cascade.detectMultiScale(gray, faces);
                Rect[] facesArray = faces.toArray();
                if (facesArray.length != 0) {
                    Rect found = facesArray[0];
                    Imgproc.rectangle(im, found.tl(), found.br(), new Scalar(0,200,0), 3);

                    Mat fi = gray.submat(found);
                    if (fi.size() != trainSize) // not needed for lbph, but for eigen and fisher
                        Imgproc.resize(fi,fi,trainSize);

                    String s = face.predict(fi);
                    if (s != "")
                        Imgproc.putText(im, s, new Point(40,40), Core.FONT_HERSHEY_PLAIN,1.3,new Scalar(0,0,200),2);
                }
            }
            frame.imshow(im);
            int k = frame.waitKey(30);
            if (k == 27) // 'esc'
                break;
            if (k == 's')
                Imgcodecs.imwrite("frame.png", im);
        }*/
        System.exit(0); // to break out of the ant shell.
    }
}