package at.vcity.androidim;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.GridView;

public class GalleryThumbnailAdapter extends BaseAdapter {
	private Context mContext;
	private ArrayList<ImageID> mData;
	private GalleryThumbnailHandler mHandler = null;

	public GalleryThumbnailAdapter(Context c, ArrayList<ImageID> data, GalleryThumbnailHandler handler) {
		mContext = c;
		mData = data;
		mHandler = handler;
	}

	@Override
	public int getCount() {
		return mData.size();
	}

	@Override
	public Object getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final GalleryThumbnailView view;

		int oldpos = -1;
		Boolean needupdate = false;

		ViewHolder vh;
		if (convertView == null) {
			view = new GalleryThumbnailView(mContext, new GalleryThumbnailView.GalleryThumbnailViewHandler() {

				@Override
				public void OnClicked(Object data, GalleryThumbnailView v) {
					ImageID img = (ImageID) data;
					img.checked = !img.checked;
					v.setChecked(img.checked);
					if (mHandler != null)
						mHandler.OnSelectionChanged(img, img.checked);
				}
			});

            WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();

            DisplayMetrics outMetrics = new DisplayMetrics();
            display.getMetrics(outMetrics);

            float screenDensity = mContext.getResources().getDisplayMetrics().density;
            int screenWidthDP = (int) ((float) outMetrics.widthPixels / screenDensity);
           // int screenHeightDP = (int) ((float) outMetrics.heightPixels / screenDensity);


			int size = (int) ((screenWidthDP / 3.0f - .6f) * screenDensity);
			view.setLayoutParams(new GridView.LayoutParams(size, size));

			vh = new ViewHolder();
			vh.thumbnail = view;
			vh.position = position;
			view.setTag(vh);

			needupdate = true;
		} else {

			view = (GalleryThumbnailView) convertView;
			vh = (ViewHolder) view.getTag();
			oldpos = vh.position;

			if (oldpos != position) {
				needupdate = true;
				//view.mThumbnail.setTag();
               // view.setThumbnail( BitmapFactory.decodeResource(mContext.getResources(), R.drawable.white));//(Bitmap) null);
				vh.position = position;
			}
		}

		final ImageID img = mData.get(position);
		view.data = img;
        view.mThumbnail.setTag(img);
		if (needupdate) {
            if(cancelPotentialWork(img.id,view))
            {
                final ConcurrentBitmapLoader task=new ConcurrentBitmapLoader(view);
                final AsyncDrawable asyncDrawable=new AsyncDrawable(mContext.getResources(),null,task);
                view.mThumbnail.setImageDrawable(asyncDrawable);
                task.execute(img.id);
            }

            //view.setThumbnail(null);
            /*new AsyncTask<GalleryThumbnailView,Void,Bitmap>(){
                private GalleryThumbnailView v;
                @Override
                protected Bitmap doInBackground(GalleryThumbnailView... params)
                {
                    v=params[0];
                    Bitmap bm = MediaStore.Images.Thumbnails.getThumbnail(mContext.getContentResolver(), img.id, MediaStore.Images.Thumbnails.MICRO_KIND, null);
                    return bm;
                }

                @Override
                protected void onPostExecute(Bitmap result)
                {
                    if(((int)v.mThumbnail.getTag())==img.id)
                    {
                        v.setThumbnail(result);
                        v.setChecked(img.checked);
                    }
                }

            }.execute(view);*/
			//Bitmap bm = MediaStore.Images.Thumbnails.getThumbnail(mContext.getContentResolver(), img.id, MediaStore.Images.Thumbnails.MICRO_KIND, null);
			//view.setThumbnail(bm);
			//view.setChecked(img.checked);
		}

		return view;
	}

	public interface GalleryThumbnailHandler {
		public void OnSelectionChanged(ImageID img, boolean checked);
	}

	public static class ImageID {
		int id;
		boolean checked;
        String path;
	}

	private static class ViewHolder {
		public GalleryThumbnailView thumbnail;
		public int position;
	}

    private class ConcurrentBitmapLoader extends AsyncTask<Integer,Void,Bitmap>{
        private WeakReference<GalleryThumbnailView> v;
        int imgid;

        public ConcurrentBitmapLoader(GalleryThumbnailView gtv)
        {
            v=new WeakReference<GalleryThumbnailView>(gtv);//imgid=i;
        }
        @Override
        protected Bitmap doInBackground(Integer... params)
        {
            imgid=params[0];
            Bitmap bm = MediaStore.Images.Thumbnails.getThumbnail(mContext.getContentResolver(), imgid, MediaStore.Images.Thumbnails.MICRO_KIND, null);
            return bm;
        }

        @Override
        protected void onPostExecute(Bitmap result)
        {
            if (isCancelled()) {
                result = null;
            }
            if(v!=null&& result!=null)
            {
                final  GalleryThumbnailView gtv=v.get();
                final ConcurrentBitmapLoader loader= getBitmapWorkerTask(gtv);
                if( this==loader && gtv!=null)
                {
                    gtv.setThumbnail(result);
                    gtv.setChecked(((ImageID)gtv.mThumbnail.getTag()).checked);
                }

            }
        }
    }

    static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<ConcurrentBitmapLoader> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap,
                             ConcurrentBitmapLoader bitmapWorkerTask) {
            super(res, bitmap);
            bitmapWorkerTaskReference =
                    new WeakReference<ConcurrentBitmapLoader>(bitmapWorkerTask);
        }

        public ConcurrentBitmapLoader getConcurrentBitmapLoader() {
            return bitmapWorkerTaskReference.get();
        }
    }

    private static ConcurrentBitmapLoader getBitmapWorkerTask(GalleryThumbnailView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.mThumbnail.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getConcurrentBitmapLoader();
            }
        }
        return null;
    }

    public static boolean cancelPotentialWork(int imgid, GalleryThumbnailView imageView) {
        final ConcurrentBitmapLoader bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null)
        {
            final int bitmapData = bitmapWorkerTask.imgid;
            if (bitmapData == 0 || bitmapData != imgid)
            {
                bitmapWorkerTask.cancel(true);
            }
            else
            {
                return false;
            }
        }
        return true;
    }
}
