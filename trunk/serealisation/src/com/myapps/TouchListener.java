package com.myapps;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.graphics.PointF;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class TouchListener implements OnTouchListener {
	private static final String TAG = "TouchLog";
	static final int NONE = 0;
	static final int DRAG = 1;
	static final int ZOOM = 2;
	public int mode = NONE;

	private CameraControl camC;
	private float width, height;
	private PointF current = new PointF(0, 0);
	private float currentDist = 0;

	public TouchListener(CameraControl pCamC) {
		camC = pCamC;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		width = v.getWidth();
		height = v.getHeight();
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			current.set(event.getX(), event.getY());
			mode = DRAG;
			Log.i(TAG, "mode=DRAG");
			break;

		case MotionEvent.ACTION_POINTER_DOWN:
			currentDist = calculateDistance(
					new PointF(event.getX(0), event.getY(0)),
					new PointF(event.getX(1), event.getY(1)));
			mode = ZOOM;
			Log.i(TAG, "mode=ZOOM");
			break;

		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_POINTER_UP:
			mode = NONE;
			Log.i(TAG, "mode=NONE");
			break;

		case MotionEvent.ACTION_MOVE:
			Log.i(TAG, "ACTION_MOVE");
			if (mode == DRAG) {
				PointF start = new PointF(current.x, current.y);
				current.set(event.getX(), event.getY());
				float moveX = scaleMoveX(calculateMoveX(start, current));
				float moveY = scaleMoveY(calculateMoveY(start, current));

				/* TODO refactoring */
				URL url = null;
				HttpURLConnection con = null;
				try {
					if (start.x - event.getX() < 0) {
						Log.i(TAG, "droite");
						url = new URL(
								"http://82.125.192.139:80/axis-cgi/com/ptz.cgi?camera=1&move=right");
					} else {
						Log.i(TAG, "gauche");
						url = new URL(
								"http://82.125.192.139:80/axis-cgi/com/ptz.cgi?camera=1&move=left");
					}
					con = (HttpURLConnection) url.openConnection();
					con.setDoOutput(true);
					con.setConnectTimeout(1000);
					con.setRequestProperty("Authorization", base64Encoder
							.userNamePasswordBase64("root", "root"));
					con.connect();
					Log.i(TAG,
							"URL " + url.toString() + " code : "
									+ con.getResponseCode());
					/* Bloc UI thread to don't spam request */
					Thread.sleep(1000);
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				// camC.changeValFunc(CameraControl.PAN, moveX, moveY);
				/* END TODO */
			} else if (mode == ZOOM) {
				float startDist = currentDist;
				currentDist = calculateDistance(
						new PointF(event.getX(0), event.getY(0)), new PointF(
								event.getX(1), event.getY(1)));
				Log.i(TAG, "currentDist=" + currentDist);
				if (currentDist > 10) {
					float ratio = (currentDist / startDist > 0) ? currentDist
							/ startDist : -1 * (startDist / currentDist);
					camC.changeValFunc(CameraControl.ZOOM, scaleZoom(ratio), 0);
				}
			}
			break;
		}

		return true;
	}

	/** Calculate the horizontal distance between 2 points */
	private float calculateMoveX(PointF a, PointF b) {
		return b.x - a.x;
	}

	/** Calculate the vertical distance between 2 points */
	private float calculateMoveY(PointF a, PointF b) {
		return b.y - a.y;
	}

	/** Calculate the distance between 2 points */
	private float calculateDistance(PointF a, PointF b) {
		float x = b.x - a.x;
		float y = b.y - a.y;
		return FloatMath.sqrt(x * x + y * y);
	}

	/** Scale screen move to real pan move performed by the camera */
	private float scaleMoveX(float distance) {
		return width / 180 * distance;
	}

	/** Scale screen move to real tilt move performed by the camera */
	private float scaleMoveY(float distance) {
		return height / 180 * distance;
	}

	/** Scale screen zoom to real zoom performed by the camera */
	private float scaleZoom(float ratio) {
		return ratio * 1000;
	}
}
