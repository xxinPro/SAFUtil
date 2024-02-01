SAFUtil
===============

## 使用范围

该工具的使用范围及其广泛，理论支持一切可以通过SAF框架访问的文件目录，经测试，包括但不限于以下目录的访问

1. Android11及以后的Android/data(obb)目录访问
2. Android系统中对扩展储存卡的访问
3. Android外接储存设备中文件目录的访问

## 引用方式

`Gradle 7.0`以下，需要在项目级`build.gradle`文件中加入

```sh
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

`Gradle 7.0`以上，需要在`setting.gradle`文件中加入

```sh
dependencyResolutionManagement {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```

远程仓库配置之后，即可在模块的`build.gradle`中引入`SAFUtil`

```sh
dependencies {
    implementation 'com.github.xxinPro:SAFUtil:1.0'
}
```

## 使用方式

### 获取操作对象

通过`XAFUtil.create(Context, String)`方法创建一个`XAFUtil`对象，第一个参数不议，第二个参数代表你想要访问的文件目录地址，下文中称作“权限目录”

```java
SAFUtil safUtil = SAFUtil.create(context, "/storage/emulated/0/Android/data/com.test.folder");
```

### 所有文件访问权限

所有文件访问权限据说能提升SAF框架的访问速度

| 方法名                                                                  | 作用             |
|----------------------------------------------------------------------|----------------|
| isManagerExternalPermission()                                        | 判断是否拥有所有文件访问权限 |
| requestManagerExternalPermission(Activity activity, int requestCode) | 申请所有文件访问权限     |

### 权限目录访问权限

创建`XAFUtil`对象时，第二个参数代表称作“权限目录”

| 方法名                                                   | 作用                                |
|-------------------------------------------------------|-----------------------------------|
| isPermission()                                        | 判断当前权限目录是否拥有访问权限                  |
| requestPermission(Activity activity, int requestCode) | 申请权限目录的访问权限（Activity中调用）          |
| requestPermission(Fragment fragment, int requestCode) | 申请权限目录的访问权限（Fragment中调用）          |
| savePermission(int requestCode, Intent intent)        | 权限申请后，返回当前Activity，调用该方法保存已经申请的权限 |

### 获取DocumentFile

由于`DocumentFile`无法像`File`对象一样通过文件路径创建并且直接操作，所以通过文件路径获取`DocumentFile`时，必须传入要获取的目标文件的`DocumentFile`是文件类型还是文件夹类型，若目标文件或文件夹不存在，会自动创建

| 方法名                                                                         | 作用                                     |
|-----------------------------------------------------------------------------|----------------------------------------|
| getDocumentFile()                                                           | 获取权限目录的DocumentFile对象                  |
| getDocumentFile(String filePath, boolean isFile)                            | 获取权限目录的子文件DocumentFile对象               |
| getDocumentFile(DocumentFile documentFile, String filePath, boolean isFile) | 获取权限目录下子DocumentFile的子文件DocumentFile对象 |

### 文件操作

对目标`DocumentFile`进行操作时，若不存在可以通过`createFile(String filePath)`创建。当然，也可以通过`getDocumentFile(String filePath, boolean isFile)`直接获取，会自动创建
注意传入的路径，一定要有其父目录的访问权限

| 方法名                                                         | 作用                         |
|-------------------------------------------------------------|----------------------------|
| createFolder(String folderPath)                             | 创建文件夹                      |
| createFile(String filePath)                                 | 创建文件                       |
| deleteFile(String filePath, boolean isFile)                 | 删除文件夹或文件                   |
| renameFile(String filePath, boolean isFile, String newName) | 重命名文件夹或文件                  |
| copyFile(DocumentFile fromFile, File toFile)                | 将DocumentFile文件复制到指定File   |
| copyFile(File fromFile, DocumentFile toFile)                | 将File复制到DocumentFile       |
| copyFile(DocumentFile fromFile, DocumentFile toFile)        | 将DocumentFile到DocumentFile |

### 文件流操作

注意传入的路径，一定要有其父目录的访问权限

| 方法名                                                           | 作用                    |
|---------------------------------------------------------------|-----------------------|
| getInputStream(String filePath)                               | 获取输入流                 |
| getInputStream(DocumentFile documentFile)                     | 获取输入流                 |
| getOutputStream(String filePath)                              | 打开输出流                 |
| getOutputStream(DocumentFile documentFile)                    | 打开输出流                 |
| getFileDescriptor(DocumentFile documentFile, String openMode) | 获取DocumentFile类型的文件描述 |

### 其他操作

| 方法名                      | 作用            |
|--------------------------|---------------|
| uriToPath(Uri uri)       | 将Uri地址为普通文件路径 |
| uriToPath(String uriStr) | 将Uri地址为普通文件路径 |


体验demo和成品aar文件存放在release中，有需要请自行下载

详细逻辑请前往[https://blog.xxin.xyz/2022/10/23/%E5%B0%81%E8%A3%85DocumentFile/](https://blog.xxin.xyz/2022/10/23/%E5%B0%81%E8%A3%85DocumentFile/)


