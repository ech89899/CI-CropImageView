# CI-CropImageView

CropImageView is based on the normal ImageView with additional cropping feature.

![Rectangle crop](/Screenshots/Screenshot_001.png) ![Circle crop](/Screenshots/Screenshot_002.png)


## How to use
The library is available on jCenter repository. To use it in your project add the following dependency in your module's build.gradle file:

```
dependencies {
    compile 'net.ciapps.widget:cropimageview:1.0.1'
}
```

Add CropImageView to your layout:

```xml
<net.ciapps.widget.CropImageView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:id="@+id/imageCrop"
    app:cropMode="circle"
    app:fixAspectRatio="true"
    app:maxCropWidth="512"
    app:maxCropHeight="512"
    app:showTouchTarget="false"
    />
```          
