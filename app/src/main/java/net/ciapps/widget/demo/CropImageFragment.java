package net.ciapps.widget.demo;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import net.ciapps.widget.CropImageView;
import net.ciapps.widget.demo.Utils.MediaFilesHelper;

import java.io.File;

public class CropImageFragment extends Fragment {
    public final static String TAG = CropImageFragment.class.getSimpleName();

    private final static String PARAMETERS_CAMERA_FILE_NAME = "CameraFileName";
    private final static String PARAMETERS_CROP_MODE = "CropMode";

    private final static int REQUEST_NEW_PICTURE = 1;
    private final static int REQUEST_SELECT_PICTURE = 2;

    int cropMode;
    String fileName;

    CropImageView imageCrop;
    Button buttonLoad;
    Button buttonCrop;

    public CropImageFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_crop_image, container, false);

        setHasOptionsMenu(true);

        Bundle args = getArguments();
        if (args != null) {
        }
        if (savedInstanceState != null) {
            restore(savedInstanceState);
        }
        else {
            initialize();
        }

        imageCrop = (CropImageView) rootView.findViewById(R.id.imageCrop);
        buttonLoad = (Button) rootView.findViewById(R.id.buttonLoad);
        buttonCrop = (Button) rootView.findViewById(R.id.buttonCrop);

        buttonLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogSelectPicture();
            }
        });
        buttonCrop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap croppedBitmap = imageCrop.getCroppedBitmap();
                imageCrop.setImageBitmap(croppedBitmap);
                imageCrop.setCropMode(CropImageView.CROP_MODE_NONE);
            }
        });
        Picasso.with(getActivity()).load(fileName).into(imageCrop);

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(PARAMETERS_CAMERA_FILE_NAME, fileName);
    }

    public void initialize() {
        fileName = "http://i.imgur.com/DvpvklR.png";
    }

    public void restore(Bundle savedInstanceState) {
        fileName = savedInstanceState.getString(PARAMETERS_CAMERA_FILE_NAME);
    }

    // Listeners
    //
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            String url;
            switch (requestCode) {
                case REQUEST_NEW_PICTURE:
                    Picasso.with(getActivity()).load(fileName).into(imageCrop);
                    imageCrop.setCropMode(cropMode);
                    return;
                case REQUEST_SELECT_PICTURE:
                    Uri uri = data.getData();
                    if (uri != null) {
                        url = MediaFilesHelper.getPath(getActivity(), uri);
                        Picasso.with(getActivity()).load("file://" + url).into(imageCrop);
                        imageCrop.setCropMode(cropMode);
                    }
                    return;
                default:
                    break;
            }
        }
        if (resultCode == Activity.RESULT_CANCELED) {
            fileName = null;
        }
    }

    private void dialogSelectPicture() {
        final String[] values = getResources().getStringArray(R.array.dialog_select_picture);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, android.R.id.text1, values) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    convertView = inflater.inflate(android.R.layout.simple_list_item_1, null);

                    TextView item = (TextView) convertView.findViewById(android.R.id.text1);
                    item.setText(values[position]);
                }

                return convertView;
            }

        };

        ListView list = new ListView(getActivity());

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(null)
                .setView(list);

        final AlertDialog dialog = builder.create();

        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    String path = getApplicationExternalDataPath();
                    if (path != null) {
                        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                        File file = new File(path, "camera_" + System.currentTimeMillis() + ".png");
                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
                        fileName = "file://" + file.getAbsolutePath();
                        startActivityForResult(cameraIntent, REQUEST_NEW_PICTURE);
                    }
                }
                if (position == 1) {
                    Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(Intent.createChooser(i, "Select picture"), REQUEST_SELECT_PICTURE);
                }

                dialog.dismiss();
            }
        });

        dialog.show();
    }

    public String getApplicationExternalDataPath() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File path = new File(Environment.getExternalStorageDirectory() + File.separator + getString(R.string.app_path));
            if (!path.exists()) {
                if (!path.mkdirs()) {
                    return null;
                }
            }
            return path.getPath();
        }
        else {
            return null;
        }
    }

    // Menu
    //
    @Override
    public void onCreateOptionsMenu (Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.crop_image, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem menuRectangle = menu.findItem(R.id.menuCropImageRectangle);
        MenuItem menuCircle = menu.findItem(R.id.menuCropImageCircle);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menuCropImageRectangle:
                cropMode = CropImageView.CROP_MODE_RECTANGLE;
                imageCrop.setCropMode(cropMode);
                imageCrop.invalidate();
                return true;
            case R.id.menuCropImageCircle:
                cropMode = CropImageView.CROP_MODE_CIRCLE;
                imageCrop.setCropMode(cropMode);
                imageCrop.invalidate();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
