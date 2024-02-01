package com.example.xafutiltest;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.documentfile.provider.DocumentFile;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import xyz.xxin.saf.SAFUtil;

public class MainActivity extends AppCompatActivity {

    private SAFUtil safUtil;

    private final int REQUEST_CODE = 10001;
    private final int REQUEST_ALL_FILE_CODE = 10002;

    private EditText input_path;
    private Button is_permission;
    private Button request_permission;
    private Button is_all_permission;
    private Button request_all_permission;
    private Button create_folder;
    private Button create_file;
    private Button write_file;
    private Button read_file;
    private Button copy_file1;
    private Button copy_file2;
    private Button copy_file3;
    private Button rename_file;
    private Button delete_file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        request();
        findView();
        initView();
        initData();
        addListener();
    }

    private void request() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this,"已经拥有读取和储存权限",Toast.LENGTH_SHORT).show();
            }
            else{
                ActivityCompat.requestPermissions(this,new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                },1);//最后一个参数是请求码
            }
        }
    }

    private void addListener() {
        is_permission.setOnClickListener(view -> {
            initData();
            toast("是否拥有访问权限 = " + safUtil.isPermission());
        });

        request_permission.setOnClickListener(view -> {
            initData();
            safUtil.requestPermission(this, REQUEST_CODE);
        });

        is_all_permission.setOnClickListener(view ->
                toast("是否拥有所有文件访问权限 = " + safUtil.isManagerExternalPermission()));

        request_all_permission.setOnClickListener(view ->
                safUtil.requestManagerExternalPermission(this, REQUEST_ALL_FILE_CODE));

        create_folder.setOnClickListener(view -> {
            String string = initData();
            safUtil.createFolder(string + "/test");
            toast("test文件夹创建成功");
        });

        create_file.setOnClickListener(view -> {
            String string = initData();
            safUtil.createFile(string + "/test/test.txt");
            toast("/test/test.txt文件创建成功");
        });

        write_file.setOnClickListener(view -> {
            String string = initData();
            String text = "测试内容";
            OutputStream outputStream = safUtil.getOutputStream(string + "/test/test.txt");
            try {
                outputStream.write(text.getBytes());
                toast("/test/test.txt写入成功");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        read_file.setOnClickListener(view -> {
            String string = initData();
            InputStream inputStream = safUtil.getInputStream(string + "/test/test.txt");
            byte[] buffer = new byte[1024];
            int len;
            StringBuilder stringBuilder = new StringBuilder();
            try {
                while ((len = inputStream.read(buffer)) != -1){
                    stringBuilder.append(new String(buffer, 0, len));
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (inputStream != null){
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            toast(stringBuilder.toString());
        });

        copy_file1.setOnClickListener(view -> {
            String string = initData();
            DocumentFile documentFile = safUtil.getDocumentFile(string + "/test/test.txt", true);
            DocumentFile documentFile2 = safUtil.getDocumentFile(string + "/test/test2.txt", true);
            safUtil.copyFile(documentFile, documentFile2);
        });

        copy_file2.setOnClickListener(view -> {
            String string = initData();
            DocumentFile documentFile = safUtil.getDocumentFile(string + "/test/test.txt", true);
            File file = new File(Environment.getExternalStorageDirectory() + "/test.txt");
            safUtil.copyFile(documentFile, file);
        });

        copy_file3.setOnClickListener(view -> {
            String string = initData();
            DocumentFile documentFile = safUtil.getDocumentFile(string + "/test/test3.txt", true);
            File file = new File(Environment.getExternalStorageDirectory() + "/test.txt");
            safUtil.copyFile(file, documentFile);
        });

        rename_file.setOnClickListener(view -> {
            String string = initData();
            safUtil.renameFile(string + "/test/test.txt", true, "test_rename");
        });

        delete_file.setOnClickListener(view -> {
            String string = initData();
            safUtil.deleteFile(string + "/test", false);
        });
    }

    private String initData() {
        safUtil = SAFUtil.create(this, input_path.getText().toString());
        return input_path.getText().toString();
    }

    private void initView() {
        input_path.setText(Environment.getExternalStorageDirectory().getPath());
    }

    private void findView() {
        input_path = findViewById(R.id.input_path);
        is_permission = findViewById(R.id.is_permission);
        request_permission = findViewById(R.id.request_permission);
        is_all_permission = findViewById(R.id.is_all_permission);
        request_all_permission = findViewById(R.id.request_all_permission);
        create_folder = findViewById(R.id.create_folder);
        create_file = findViewById(R.id.create_file);
        write_file = findViewById(R.id.write_file);
        read_file = findViewById(R.id.read_file);
        rename_file = findViewById(R.id.rename_file);
        delete_file = findViewById(R.id.delete_file);
        copy_file1 = findViewById(R.id.copy_file1);
        copy_file2 = findViewById(R.id.copy_file2);
        copy_file3 = findViewById(R.id.copy_file3);
    }

    private void toast(String text){
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        safUtil.savePermission(requestCode, data);
    }
}



