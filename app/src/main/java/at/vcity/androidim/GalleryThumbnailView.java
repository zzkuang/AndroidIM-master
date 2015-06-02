package at.vcity.androidim;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class GalleryThumbnailView extends RelativeLayout {

	public ImageView mThumbnail;
	private ImageView mCheck;
	private GalleryThumbnailViewHandler mHandler;
	public Object data;

	public GalleryThumbnailView(Context context, GalleryThumbnailViewHandler handler) {
		super(context);

		mHandler = handler;

		LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(R.layout.gallery_thumbnail_entry, this);

		mThumbnail = (ImageView) findViewById(R.id.gallery_thumb);
		mCheck = (ImageView) findViewById(R.id.galthumb_tick);

		Button fbbut = (Button) findViewById(R.id.feedbackbutton);
		fbbut.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mHandler != null) {
					mHandler.OnClicked(data, GalleryThumbnailView.this);
				}
			}
		});
	}

	public void setThumbnail(Bitmap thumbnail) {
		mThumbnail.setImageBitmap(thumbnail);
	}

	public interface GalleryThumbnailViewHandler {
		public void OnClicked(Object data, GalleryThumbnailView v);
	}

	public void setChecked(boolean checked) {
		if (checked) {
			mCheck.setImageDrawable(getResources().getDrawable(R.drawable.tick));
			//mCheck.setAlpha(1.0f);
		} else {
			mCheck.setImageDrawable(getResources().getDrawable(R.drawable.tick_gray));
			//mCheck.setAlpha(0.7f);
		}
	}
}