package at.vcity.androidim;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import at.vcity.androidim.GalleryThumbnailAdapter.ImageID;

public class Gallery extends Activity {

	private HashSet<ImageID> mSelected = new HashSet<ImageID>();
	ArrayList<GalleryThumbnailAdapter.ImageID> imageIDs = new ArrayList<GalleryThumbnailAdapter.ImageID>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.select_image_gallery);

		String[] projection = { MediaStore.Images.ImageColumns._ID, MediaStore.Images.Thumbnails.DATA, MediaStore.Images.ImageColumns.DATE_TAKEN };

		Cursor d = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, "", null, null);

		//d.moveToFirst();
        d.moveToLast();
		for (int i = 0; i < d.getCount(); i++) {
			GalleryThumbnailAdapter.ImageID img = new GalleryThumbnailAdapter.ImageID();
			img.checked = false;
            img.path=d.getString(d.getColumnIndex(MediaStore.Images.Media.DATA));
			img.id = d.getInt(0);
			imageIDs.add(img);
			//d.moveToNext();
            d.moveToPrevious();
		}

		final TextView numselected = (TextView) findViewById(R.id.num_selected);

		final GridView grid = (GridView) findViewById(R.id.gallery_grid);
		final Button okaybutton = (Button) findViewById(R.id.okaybutton);
		final Button clearAll = (Button) findViewById(R.id.clearAll);

		okaybutton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
                ArrayList<String> pathlist=new ArrayList<String>();
                for(ImageID imageID: mSelected)
                {
                  //  PropertyPicture p=new PropertyPicture();
                   // p.onServer = false;
                   // p.id = UUID.randomUUID();
                   // Global.CurrentProperty.photos.add(p);
                    pathlist.add(imageID.path);
                }
                getIntent().putExtra("ImagePaths",pathlist);
                setResult(RESULT_OK, getIntent());
                finish();
			}
        });

		clearAll.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				mSelected.clear();
				for (ImageID img : imageIDs)
					img.checked = false;
				okaybutton.setEnabled(false);
				clearAll.setVisibility(View.INVISIBLE);
				numselected.setVisibility(View.INVISIBLE);
				numselected.setText("");
				for (int i = 0; i < grid.getChildCount(); i++) {
					GalleryThumbnailView gtv = (GalleryThumbnailView) grid.getChildAt(i);
					gtv.setChecked(false);
				}

			}
		});

		GalleryThumbnailAdapter gta = new GalleryThumbnailAdapter(this, imageIDs, new GalleryThumbnailAdapter.GalleryThumbnailHandler() {
			@Override
			public void OnSelectionChanged(ImageID img, boolean checked) {
				if (checked)
                {
                    mSelected.add(img);
                }
				else
					mSelected.remove(img);
				if (mSelected.size() > 0) {
					okaybutton.setEnabled(true);
					clearAll.setVisibility(View.VISIBLE);
					numselected.setVisibility(View.VISIBLE);
					numselected.setText("已選" + Integer.toString(mSelected.size()) + "張");
				} else {
					okaybutton.setEnabled(false);
					clearAll.setVisibility(View.INVISIBLE);
					numselected.setVisibility(View.INVISIBLE);
					numselected.setText("");
				}
			}
		});
		grid.setAdapter(gta);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.gallery, menu);
		return true;
	}

}
