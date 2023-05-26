package com.example.myanimalmatch;



import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

public class ResultActivity extends AppCompatActivity {
    private ImageView imageView;
    private TextView resultText;
    private Button button;
    private ImageView animal;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        imageView = findViewById(R.id.imageView);
        resultText = findViewById(R.id.resultText);
        button = findViewById(R.id.button);
        animal = findViewById(R.id.animal);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish(); // 현재 액티비티 종료
            }
        });

        // MainActivity에서 전달받은 사진의 경로
        String uploadedImagePath = getIntent().getStringExtra("currentPhotoPath");
        String result = getIntent().getStringExtra("response");
        // 경로를 사용하여 이미지를 로드하여 ImageView에 표시
        setResultText(result);
        loadImage(uploadedImagePath);

    }

    private void loadImage(String uploadedImagePath) {
        try {
            // 사진 파일을 Bitmap으로 로드
            Bitmap bitmap = BitmapFactory.decodeFile(uploadedImagePath);

            // 사진의 EXIF 회전 정보를 확인하여 회전 각도를 가져옴
            ExifInterface exifInterface = new ExifInterface(uploadedImagePath);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            // 회전 각도에 따라 사진을 회전시킴
            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
                default:
                    break;
            }
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            // 회전된 이미지를 ImageView에 설정하여 표시
            imageView.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // 결과 텍스트를 설정하는 메서드
    private void setResultText(String result) {
        resultText.setText(result);
        try {
            JSONObject responseJson = new JSONObject(result);
            JSONArray predictionsArray = responseJson.getJSONArray("predictions");

            // probability가 가장 높은 값을 찾습니다.
            String highestProbabilityClass = "";
            double highestProbability = 0;

            JSONObject predictionObject1 = predictionsArray.getJSONObject(0);
            highestProbabilityClass = predictionObject1.getString("class");
            highestProbability = Double.parseDouble(predictionObject1.getString("probability"));

            // 화면에 결과를 표시하거나 처리할 로직을 추가합니다.
            // 예: TextView에 결과를 설정한다면
            resultText.setText(highestProbabilityClass + "(" + highestProbability + ")%");
            if (highestProbabilityClass.equals("강아지")) {
                animal.setImageResource(R.drawable.dog);
            }
            else if (highestProbabilityClass.equals("곰")) {
                animal.setImageResource(R.drawable.bear);
            }
            else if (highestProbabilityClass.equals("고양이")) {
                animal.setImageResource(R.drawable.cat);
            }
            else if (highestProbabilityClass.equals("사슴")) {
                animal.setImageResource(R.drawable.deer);
            }
            else if (highestProbabilityClass.equals("여우")) {
                animal.setImageResource(R.drawable.fox);
            }
            else if (highestProbabilityClass.equals("토끼")) {
                animal.setImageResource(R.drawable.rabbit);
            }
            else if (highestProbabilityClass.equals("뱀")) {
                animal.setImageResource(R.drawable.snake);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


}
