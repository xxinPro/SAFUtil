package xyz.xxin.xaf;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class XAFUtil {
    private static final String TAG = XAFUtil.class.getSimpleName();

    private int requestCode;                // 请求码

    private final Context context;          // 上下文

    private final String URI_HEAD;          // uri地址头，任何一个DocumentFile的Uri地址都包含这个地址头

    private final String permissionPath;    // 请求权限的目录地址（如：storage/sdcard/test）
    private final String permissionUriStr;  // 请求权限的目录的uri地址（该Uri地址仅用于申请权限，切勿直接操作）

    public final static String PRIMARY_STORAGE;     // 主储存目录:   /storage/emulated/0
    public final static String ANDROID_PATH;        // Android目录: /storage/emulated/0/Android
    public final static String ANDROID_DATA_PATH;   // data目录:    /storage/emulated/0/Android/data
    public final static String ANDROID_OBB_PATH;    // obb目录:     /storage/emulated/0/Android/obb

    {
        URI_HEAD = "content://com.android.externalstorage.documents/tree/";
    }

    static {
        // 一般来说主储存目录是/storage/emulated/0
        PRIMARY_STORAGE = Environment.getExternalStorageDirectory().getAbsolutePath();
        ANDROID_PATH = PRIMARY_STORAGE + "/Android";
        ANDROID_DATA_PATH = ANDROID_PATH + "/data";
        ANDROID_OBB_PATH = ANDROID_PATH + "/obb";
    }

    /**
     * @param context       上下文
     * @param permissionDir 请求权限的目录
     */
    public static XAFUtil create(Context context, String permissionDir) {
        return new XAFUtil(context, permissionDir);
    }

    /**
     * @param context       上下文
     * @param permissionDir 请求权限的目录
     */
    private XAFUtil(Context context, String permissionDir) {
        this.permissionPath = addSlash(permissionDir);
        this.permissionUriStr = pathToUri(permissionDir);
        this.context = context;

        // 错误时提示
        if (this.permissionUriStr == null)
            Log.e(TAG, "DocumentFileUtils: root directory permissionDir field");
    }

    /**
     * 是否拥有所有文件访问权限，安卓11之前无需申请
     */
    public boolean isManagerExternalPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return true;
    }

    /**
     * 申请所有文件访问权限，安卓11之前无需申请
     *
     * @param activity    上下文
     * @param requestCode 请求权限请求码
     */
    @SuppressLint("InlinedApi")
    public void requestManagerExternalPermission(Activity activity, int requestCode) {
        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
        intent.setData(Uri.parse("package:" + activity.getPackageName()));
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * 是否拥有所所传入的目录的访问权限
     */
    public boolean isPermission() {
        if (permissionUriStr == null) Log.e(TAG, "isPermission: root directory path field");

        Uri uriPath = Uri.parse(permissionUriStr);

        DocumentFile documentFile = DocumentFile.fromTreeUri(this.context, uriPath);
        if (documentFile != null) {
            return documentFile.canWrite();
        }
        return false;
    }

    /**
     * 请求所传入目录的访问权限
     *
     * @param activity    调用的activity
     * @param requestCode 请求码
     */
    public void requestPermission(Activity activity, int requestCode) {
        requestPermission(activity, null, requestCode);
    }

    /**
     * 请求所传入目录的访问权限
     *
     * @param fragment    调用的fragment
     * @param requestCode 请求码
     */
    public void requestPermission(Fragment fragment, int requestCode) {
        requestPermission(null, fragment, requestCode);
    }

    private void requestPermission(Activity activity, Fragment fragment, int requestCode) {
        if (permissionUriStr == null) { // 请求权限的目录的uri地址错误
            Log.e(TAG, "requestPermission: permission directory path field");
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) { // 系统版本过低
            Log.e(TAG, "requestPermission: sdk version too low");
            return;
        }

        // 请求码
        this.requestCode = requestCode;
        // 将请求权限的目录的uri地址转换为Uri对象
        Uri uriPath = Uri.parse(permissionUriStr);
        // 通过Uri对象得到DocumentFile对象，该对象只能在申请权限时使用，不可以直接读写，权限目录除外
        DocumentFile documentFile = DocumentFile.fromTreeUri(this.context, uriPath);
        if (documentFile != null) {
            Intent intent = createIntent(documentFile);
            if (activity != null) {
                activity.startActivityForResult(intent, requestCode);
            } else {
                fragment.startActivityForResult(intent, requestCode);
            }
        } else {
            Log.e(TAG, "requestPermission: " + permissionUriStr + " not exists");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private Intent createIntent(DocumentFile documentFile) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, documentFile.getUri());
        return intent;
    }

    /**
     * 请求权限后，在onActivityResult中调用保存
     *
     * @param requestCode 请求码
     * @param intent      请求数据
     */
    @SuppressLint("WrongConstant")
    public void savePermission(int requestCode, Intent intent) {
        if (intent == null) return;
        if (this.requestCode == requestCode) {
            Uri uri = intent.getData();
            if (uri != null) {
                DocumentFile documentFile = DocumentFile.fromTreeUri(context, uri);
                if (documentFile != null && documentFile.canWrite()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        this.context.getContentResolver().takePersistableUriPermission(uri,
                                intent.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
                    } else {
                        Log.e(TAG, "savePermission: sdk version too low");
                    }
                } else {
                    Log.e(TAG, "savePermission: no write permission");
                }
            } else {
                Log.e(TAG, "savePermission: data uri field");
            }
        } else {
            Log.e(TAG, "savePermission: requestCode field");
        }
    }

    /**
     * 获取权限目录的DocumentFile对象
     */
    public DocumentFile getDocumentFile() {
        return DocumentFile.fromTreeUri(context, Uri.parse(permissionUriStr));
    }

    /**
     * 获取某文件或者目录的DocumentFile对象
     *
     * @param filePath 目录或者文件路径
     * @param isFile   目标是否是文件类型，如果是文件夹类型则传入false
     * @return DocumentFile对象
     */
    public DocumentFile getDocumentFile(String filePath, boolean isFile) {
        // 在地址头和地址尾添加斜杠
        filePath = addSlash(filePath);

        // 将文件路径转换为uri地址
        String _uriPathStr = pathToUri(filePath);

        // 文件uri地址为空或者文件不属于权限目录时
        if (_uriPathStr == null || !_uriPathStr.startsWith(permissionUriStr)) return null;

        // 权限目录DocumentFile对象
        DocumentFile documentFile = DocumentFile.fromTreeUri(context, Uri.parse(permissionUriStr));

        // uri地址与权限目录的uri地址相同时，直接把权限目录的DocumentFile对象return出去
        if (_uriPathStr.equals(permissionUriStr)) return documentFile;

        // 去除与权限目录一样的部分，仅保留权限目录下的文件或文件夹路径
        String pathContent = filePath.substring(this.permissionPath.length());
        return getDocumentFile(documentFile, pathContent, isFile);
    }

    /**
     * 获取目录DocumentFile对象下的目录或文件的DocumentFile对象，在搞懂这个方法前慎用
     * 例：传入/storage/sdCard/的DocumentFile和/test/1.txt，那就是获取/storage/sdCard/test/1.txt的DocumentFile对象
     * 注意，当路径中的文件或者文件夹不存在时，该方法会自动创建
     *
     * @param documentFile DocumentFile对象
     * @param filePath     DocumentFile对象下的目录或文件路径
     * @param isFile       路径是否是文件类型，如果是文件夹类型则传入false，反之true
     */
    public DocumentFile getDocumentFile(DocumentFile documentFile, String filePath, boolean isFile) {
        // 如果documentFile有问题
        if (documentFile == null) return null;

        // 移除头尾的斜杠
        filePath = removeSlash(filePath);

        // 如果路径是空的
        if (TextUtils.isEmpty(filePath)) return documentFile;

        // 根据层级分隔符，将路径分开
        String[] pathArr = filePath.split("/");

        DocumentFile[] documentFiles = documentFile.listFiles();
        // 路径完整的情况下
        if (pathArr.length > 0) {
            // 从路径中去除掉pathArr[0]
            filePath = filePath.substring(pathArr[0].length());
            for (DocumentFile _documentFile : documentFiles) {
                if (_documentFile.getName() != null && _documentFile.getName().equals(pathArr[0])) {
                    return getDocumentFile(_documentFile, filePath, isFile);
                }
            }
            // 代码执行到这里表明文件夹中不存在指定的下一级文件夹/文件，需要我们创建一个
            // 如果pathArr.length为1，说明只剩下最后一个文件夹或文件没有找到，反之则一定为文件夹，创建文件夹即可
            // 如果指定的目标类型为文件，则创建文件，反之创建文件夹
            if (pathArr.length == 1 && isFile) {
                return documentFile.createFile("", pathArr[0]);
            } else {
                DocumentFile createDir = documentFile.createDirectory(pathArr[0]);
                return getDocumentFile(createDir, filePath, isFile);
            }
        }
        return documentFile;
    }

    /**
     * 创建文件夹
     *
     * @param folderPath 文件夹路径
     */
    public DocumentFile createFolder(String folderPath) {
        return getDocumentFile(folderPath, false);
    }

    /**
     * 创建文件
     *
     * @param filePath 文件路径
     */
    public DocumentFile createFile(String filePath) {
        return getDocumentFile(filePath, true);
    }

    /**
     * 删除文件或文件夹
     *
     * @param filePath 文件/文件夹路径
     * @param isFile   是否是文件
     * @return 删除结果
     */
    public boolean deleteFile(String filePath, boolean isFile) {
        return getDocumentFile(filePath, isFile).delete();
    }

    /**
     * 文件、文件夹重命名
     *
     * @param filePath 文件/文件夹路径
     * @param isFile   是否是文件类型
     * @param newName  新文件名
     * @return 重命名结果
     */
    public boolean renameFile(String filePath, boolean isFile, String newName) {
        return getDocumentFile(filePath, isFile).renameTo(newName);
    }

    /**
     * 将DocumentFile文件复制到File
     *
     * @param fromFile 源文件
     * @param toFile   目标文件
     */
    public void copyFile(DocumentFile fromFile, File toFile) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(fromFile.getUri());
            FileOutputStream fileOutputStream = new FileOutputStream(toFile);
            copy(inputStream, fileOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 将File复制到DocumentFile
     *
     * @param fromFile 源文件
     * @param toFile   目标文件
     */
    public void copyFile(File fromFile, DocumentFile toFile) {
        try {
            FileInputStream fileInputStream = new FileInputStream(fromFile);
            OutputStream outputStream = context.getContentResolver().openOutputStream(toFile.getUri());
            copy(fileInputStream, outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 将DocumentFile到DocumentFile
     *
     * @param fromFile 源文件
     * @param toFile   目标文件
     */
    public void copyFile(DocumentFile fromFile, DocumentFile toFile) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(fromFile.getUri());
            OutputStream outputStream = context.getContentResolver().openOutputStream(toFile.getUri());
            copy(inputStream, outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 写入数据
     *
     * @param inputStream  输入流
     * @param outputStream 输出流
     */
    private void copy(InputStream inputStream, OutputStream outputStream) {
        try {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 获取输入流
     *
     * @param filePath 文件地址
     */
    public InputStream getInputStream(String filePath) {
        DocumentFile documentFile = getDocumentFile(filePath, true);
        return getInputStream(documentFile);
    }

    /**
     * 获取输入流
     *
     * @param documentFile 文件
     */
    public InputStream getInputStream(DocumentFile documentFile) {
        try {
            return context.getContentResolver().openInputStream(documentFile.getUri());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 打开输出流
     *
     * @param filePath 文件路径
     */
    public OutputStream getOutputStream(String filePath) {
        DocumentFile documentFile = getDocumentFile(filePath, true);
        return getOutputStream(documentFile);
    }

    /**
     * 打开输出流
     *
     * @param documentFile 文件
     */
    public OutputStream getOutputStream(DocumentFile documentFile) {
        try {
            return context.getContentResolver().openOutputStream(documentFile.getUri());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取DocumentFile类型的文件描述
     *
     * @param documentFile 文件的DocumentFile对象
     * @param openMode     打开文件的模式，一般情况下w是写模式，r是读模式
     */
    public ParcelFileDescriptor getFileDescriptor(DocumentFile documentFile, String openMode) {
        Uri uri = documentFile.getUri();
        try {
            // 以写模式打开
            return context.getContentResolver().openFileDescriptor(uri, openMode);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 移除地址头和地址尾的斜杠
     *
     * @param path 文件地址
     */
    private String removeSlash(String path) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        return path;
    }

    /**
     * 在地址头和地址尾添加斜线
     *
     * @param permissionDir 请求权限的目录
     */
    private String addSlash(String permissionDir) {
        if (!permissionDir.startsWith("/")) {
            permissionDir = "/" + permissionDir;
        }

        if (!permissionDir.endsWith("/")) {
            permissionDir = permissionDir + "/";
        }
        return permissionDir;
    }

    /**
     * 将目录地址转换为uri地址，此处要求绝对正确的完整的目录地址
     * <p>
     * 转换后的Uri地址仅申请权限时可用，不可以直接使用该Uri地址转换为DocumentFile
     * 注：储存器（外置TF卡、内置SD卡都称为储存器）根目录除外
     *
     * @param path 文件路径，注意一定要传入文件的完整的绝对路径
     * @return 格式化后的Uri地址字符串
     */
    private String pathToUri(String path) {
        // 在头尾添加斜杠
        path = addSlash(path);

        // 定义路径头规范，一般路径头是/storage/
        // 例1：/storage/6238-3332/               => /storage/
        // 例2：/storage/6238-3332/Android/       => /storage/
        // 例3：/storage/emulated/0/Android/data/ => /storage/
        String pathHead = PRIMARY_STORAGE.substring(0, PRIMARY_STORAGE.indexOf("/", 1) + 1);
        // 如果传入的路径头与规范头不同，说明路径不对
        if (!path.startsWith(pathHead)) return null;

        // 这一步去除路径头，假设规范路径头是/storage/
        // 例1：/storage/6238-3332/               => 6238-3332/
        // 例2：/storage/6238-3332/Android/       => 6238-3332/Android/
        // 例3：/storage/emulated/0/Android/data/ => emulated/0/Android/data/
        String pathContent = path.substring(pathHead.length());

        // 取/storage/下的主储存目录，一般主储存目录是emulated/0，不需要考虑路径头不属于主储存目录的情况
        // 例1：/storage/emulated/0 => emulated/0
        String primaryPath = PRIMARY_STORAGE.substring(pathHead.length());

        // 如果传入的目录是Android内置SD卡下的主目录，假设主目录是emulated/0，将传入路径中的emulated/0替换为primary
        // 例1：emulated/0/Android/data/ => primary/Android/data/
        if (pathContent.startsWith(primaryPath))
            pathContent = "primary" + pathContent.substring(primaryPath.length());

        // 拿到储存器目录的目录名
        // 理论上讲，所传入的目录路径中，/storage/的直接子目录就是储存器（外置TF卡、内置SD卡都称为储存器）目录的目录名
        // 例1：6238-3332/            => 6238-3332
        // 例2：6238-3332/Android/    => 6238-3332
        // 例3：primary/Android/data/ => primary
        String rootPathName = pathContent.substring(0, pathContent.indexOf("/"));

        // 从路径中剔除储存器目录
        // 例1：6238-3332/            => /
        // 例2：6238-3332/Android/    => Android/
        // 例3：primary/Android/data/ => Android/data/
        pathContent = pathContent.substring(rootPathName.length() + 1);

        // 去除末尾的“/”
        // 例1：/             =>
        // 例2：Android/      => Android
        // 例3：Android/data/ => Android/data
        if (pathContent.endsWith("/"))
            pathContent = pathContent.substring(0, pathContent.length() - 1);

        // 将目录路径中的/全部替换为%2F
        // 例1：              =>
        // 例2：Android       => Android
        // 例2：Android/data  => Android%2Fdata
        pathContent = pathContent.replaceAll("/", "%2F");

        // 得到完整Uri地址
        return URI_HEAD + rootPathName + "%3A" + pathContent;
    }

    /**
     * 将Uri地址为普通文件路径
     *
     * @param uri uri地址
     * @return 普通文件路径
     */
    public String uriToPath(Uri uri) {
        return uriToPath(uri.toString());
    }

    /**
     * 将Uri地址为普通文件路径
     *
     * @param uriStr uri地址
     * @return 普通文件路径
     */
    public String uriToPath(String uriStr) {
        String colon = "%3A";   // %3A代表冒号
        String slash = "%2F";   // %2F代表斜杠

        if (!uriStr.startsWith(URI_HEAD)) return URI_HEAD;

        // 截取目录分支
        String dirBranch = uriStr.substring(URI_HEAD.length(), uriStr.indexOf(colon));

        String branchPath;
        String dirBranchHead = "/document/" + dirBranch + colon;
        if (uriStr.contains(dirBranchHead)) {
            branchPath = uriStr.substring(uriStr.indexOf(dirBranchHead) + dirBranchHead.length()).replaceAll(slash, "/");
        } else {
            // 得到在内存中的储存路径
            branchPath = uriStr.substring(uriStr.indexOf(colon)+ colon.length()).replaceAll(slash, "/");
        }

        // 判断目录是否是Android内置SD卡下的目录，如果是将primary替换为emulated/0，primary表示主目录
        if (dirBranch.equals("primary"))
            dirBranch = "emulated/0";

        // 得到真实路径
        return "/storage/" + dirBranch + "/" + branchPath;
    }
}
