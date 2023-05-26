package com.example.myanimalmatch;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;

    private Uri selectedImage;
    public String currentPhotoPath;
    Intent resultIntent;
    Button album;
    Button btnTakePicture;
    // Retrofit 인터페이스 정의
    public interface ApiService {
        @Multipart
        @POST("animal/predict")
        Call<ResponseBody> uploadImage(@Part MultipartBody.Part image);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        resultIntent = new Intent(MainActivity.this, ResultActivity.class);
        btnTakePicture = findViewById(R.id.btnTakePicture);
        album = findViewById(R.id.album);
        btnTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });
        album.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "갤러리에서 사용할 사진을 골라주세요", Toast.LENGTH_SHORT).show();
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, REQUEST_IMAGE_PICK);
            }
        });
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
                Toast.makeText(this, "Error occurred while creating the file", Toast.LENGTH_SHORT).show();
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,"com.example.android.teamproject", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // 이미지 촬영 및 서버 전송 처리
            resultIntent.putExtra("currentPhotoPath", currentPhotoPath);
            uploadImageToServer(currentPhotoPath);
        }
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            selectedImage = data.getData();
            uploadImageToServer(uri2path(getApplicationContext(), selectedImage));
        }
    }

    private void uploadImageToServer(String imagePath) {
        Toast.makeText(getApplicationContext(), "닮은 동물 계산중...", Toast.LENGTH_SHORT).show();
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        httpClientBuilder.addInterceptor(loggingInterceptor);
        // Retrofit.Builder를 생성하고 OkHttpClient를 설정합니다
        Retrofit.Builder retrofitBuilder = new Retrofit.Builder()
                .baseUrl("https://api.wsbox.pw/")
                .client(httpClientBuilder.build())
                .addConverterFactory(GsonConverterFactory.create());
        // Retrofit 객체 생성
        Retrofit retrofit = retrofitBuilder.build();

        // MultipartBody.Part 객체 생성 (이미지 파일 전송을 위한 설정)
        File file = new File(imagePath);
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpg"), file);

        MultipartBody.Part imagePart = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        // Retrofit 인터페이스의 uploadImage 메서드 호출
        ApiService apiService = retrofit.create(ApiService.class);
        Call<ResponseBody> call = apiService.uploadImage(imagePart);
        // 비동기적으로 서버에 요청 보내기
        call.enqueue(new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                // 서버 응답 처리
                if (response.isSuccessful()) {
                    // 성공적으로 업로드되었을 때 처리할 코드 작성
                    try {
                        resultIntent.putExtra("response", response.body().string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    startActivity(resultIntent);
                } else {
                    // 업로드 실패 시 처리할 코드 작성
                    Toast.makeText(getApplicationContext(), "사진에서 얼굴을 인식할수 없습니다", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // 네트워크 오류 등 요청 실패 시 처리할 코드 작성
            }
        });
    }
    //Uri -> Path(파일경로)
    public String uri2path(Context context, Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };

        Cursor cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
        cursor.moveToNext();
        @SuppressLint("Range") String path = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA));
        resultIntent.putExtra("currentPhotoPath", path);
        cursor.close();
        return path;
    }
}
