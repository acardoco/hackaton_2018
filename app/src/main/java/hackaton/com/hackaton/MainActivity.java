package hackaton.com.hackaton;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.ibm.watson.developer_cloud.android.library.camera.CameraHelper;
import com.ibm.watson.developer_cloud.visual_recognition.v3.VisualRecognition;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifyImagesOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ImageClassification;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.VisualClassification;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.VisualClassifier;

import java.io.File;
import java.math.BigDecimal;
import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {

    private VisualRecognition vrClient;
    private CameraHelper helper;

    private static BigDecimal truncateDecimal(double x,int numberofDecimals)
    {
        if ( x > 0) {
            return new BigDecimal(String.valueOf(x)).setScale(numberofDecimals, BigDecimal.ROUND_FLOOR);
        } else {
            return new BigDecimal(String.valueOf(x)).setScale(numberofDecimals, BigDecimal.ROUND_CEILING);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vrClient = new VisualRecognition(
                VisualRecognition.VERSION_DATE_2016_05_20,
                getString(R.string.api_key)
        );

        helper = new CameraHelper(this);
    }

    public void takePicture(View view) {
        helper.dispatchTakePictureIntent();
    }

    public void chooseImage(String clase){
        ImageView imgView=(ImageView) findViewById(R.id.imageView);
        if (clase.equals("Malignant")) {
            Drawable drawable = getResources().getDrawable(R.mipmap.maligno);
            imgView.setImageDrawable(drawable);
        }
        if (clase.equals("Bening")) {
            Drawable drawable = getResources().getDrawable(R.mipmap.benigno);
            imgView.setImageDrawable(drawable);
        }

        if (clase.equals("No-lesion")) {
            Drawable drawable = getResources().getDrawable(R.mipmap.no_lesion);
            imgView.setImageDrawable(drawable);
        }
    }

    public StringBuffer resultString(String clase, BigDecimal prob){
        String clase_traduc = null;

        StringBuffer vuelta = new StringBuffer();

        if (clase.equals("Malignant")) clase_traduc = "maligna.";
        if (clase.equals("Benign")) clase_traduc = "benigna.";
        if (clase.equals("No-lesion"))
            vuelta.append("Usted no tiene ninguna lesión");
        else
            vuelta.append("Con probabilidad ").append(prob).append(" usted tiene una lesión de tipo ").append(clase_traduc);

        return vuelta;
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == CameraHelper.REQUEST_IMAGE_CAPTURE) {
            final Bitmap photo = helper.getBitmap(resultCode);
            final File photoFile = helper.getFile(resultCode);
            ImageView preview = findViewById(R.id.preview);
            preview.setImageBitmap(photo);

            AsyncTask.execute(new Runnable() {

                String resultado;
                @Override
                public void run() {
                    VisualClassification response =
                            vrClient.classify(
                                    new ClassifyImagesOptions.Builder()
                                            .images(photoFile)
                                            .classifierIds("SCD_311897649")
                                            //.classifierIds("food")
                                            .threshold(0.5) //en 0.6 muestra no lesion
                                            .build()
                            ).execute();

                    ImageClassification classification = response.getImages().get(0);
                    VisualClassifier classifier = null;
                    final StringBuffer output = new StringBuffer();

                    if (!classification.getClassifiers().isEmpty()) {
                        classifier = classification.getClassifiers().get(0);

                        for (VisualClassifier.VisualClass object : classifier.getClasses()) {
                            //if (object.getScore() > 0.7f)
                            Double prob = object.getScore();
                            prob = prob * 100;
                            BigDecimal probTruncada = truncateDecimal(prob,2);
                            output.append(resultString(object.getName(),probTruncada));
                            resultado = object.getName();
                        }
                    }
                    else {
                        resultado = "No-lesion";
                        output.append(resultString(resultado,null));
                        }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView detectedObjects = findViewById(R.id.detected_objects);
                            detectedObjects.setText(output);
                            chooseImage(resultado);
                        }


                    });

                }
            });
        }
    }

}
