package com.jamid.eastyliantest.views.zoomable

import android.graphics.PointF
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewGroup
import com.jamid.eastyliantest.interfaces.OnScaleListener
import com.jamid.eastyliantest.utility.updateLayout

class TapListener(zoomableDraweeView: ZoomableDraweeView):
	GestureDetector.SimpleOnGestureListener() {

	private val mDraweeView: ZoomableDraweeView = zoomableDraweeView
	private val mDoubleTapViewPoint = PointF()
	private val mDoubleTapImagePoint = PointF()
	private var mDoubleTapScale = 1f
	private var mDoubleTapScroll = false
	private var mScaleListener: OnScaleListener? = null

	override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
		if (e != null) {
			when (e.actionMasked) {
				MotionEvent.ACTION_DOWN -> {
					mDraweeView.performClick()
				}
				MotionEvent.ACTION_MOVE -> {

				}
				MotionEvent.ACTION_UP -> {
					mDraweeView.performClick()
				}
			}
		}
		return super.onSingleTapConfirmed(e)
	}

	override fun onDown(e: MotionEvent?): Boolean {
		Log.d("TAG", "onDown")
		return super.onDown(e)
	}

	override fun onSingleTapUp(e: MotionEvent?): Boolean {
		Log.d("TAG", "onSingleTapUp")
		return super.onSingleTapUp(e)
	}

	override fun onDoubleTapEvent(e: MotionEvent): Boolean {
		mDraweeView.updateLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
		val zc = mDraweeView.zoomableController as AbstractAnimatedZoomableController
		val vp = PointF(e.x, e.y)
		val ip = zc.mapViewToImage(vp)

		when (e.actionMasked) {
			MotionEvent.ACTION_DOWN -> {
				mDoubleTapViewPoint.set(vp)
				mDoubleTapImagePoint.set(ip)
				mDoubleTapScale = zc.scaleFactor
			}
			MotionEvent.ACTION_MOVE -> {
				mDoubleTapScroll = mDoubleTapScroll || shouldStartDoubleTapScroll(vp)
				if (mDoubleTapScroll) {
					val scale = calcScale(vp)
					zc.zoomToPoint(scale, mDoubleTapImagePoint, mDoubleTapViewPoint)
				}
			}
			MotionEvent.ACTION_UP -> {
				if (mDoubleTapScroll) {
					val scale = calcScale(vp)
					zc.zoomToPoint(scale, mDoubleTapImagePoint, mDoubleTapViewPoint)
				} else {
					val maxScale = zc.maxScaleFactor
					val minScale = zc.minScaleFactor
					if (zc.scaleFactor < (maxScale + minScale) / 2) {
						zc.zoomToPointX(
							maxScale,
							ip,
							vp,
							DefaultZoomableController.LIMIT_ALL,
							DURATION_MS.toLong(),
							null
						)
					} else {
						zc.zoomToPointX(
							minScale,
							ip,
							vp,
							DefaultZoomableController.LIMIT_ALL,
							DURATION_MS.toLong(),
							null
						)
					}
				}
				mDoubleTapScroll = false
			}
		}
		return true
	}

	private fun shouldStartDoubleTapScroll(viewPoint: PointF): Boolean {
		val dist = Math.hypot(
			(viewPoint.x - mDoubleTapViewPoint.x).toDouble(),
			(viewPoint.y - mDoubleTapViewPoint.y).toDouble()
		)
		return dist > DOUBLE_TAP_SCROLL_THRESHOLD
	}

	private fun calcScale(currentViewPoint: PointF): Float {
		val dy = currentViewPoint.y - mDoubleTapViewPoint.y
		val t = 1 + Math.abs(dy) * 0.001f
		return if (dy < 0) mDoubleTapScale / t else mDoubleTapScale * t
	}

	companion object {
		private const val DURATION_MS = 300
		private const val DOUBLE_TAP_SCROLL_THRESHOLD = 20
	}

}