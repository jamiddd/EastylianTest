package com.jamid.eastyliantest.views.zoomable

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.request.ImageRequest
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.databinding.FragmentImageViewBinding
import com.jamid.eastyliantest.interfaces.OnScaleListener
import com.jamid.eastyliantest.ui.MainViewModel
import com.jamid.eastyliantest.utility.convertDpToPx
import com.jamid.eastyliantest.utility.updateLayout

class ImageViewFragment : Fragment(R.layout.fragment_image_view), OnScaleListener {

    private var transitionName: String? = null
    private var params = Pair(0, 0)
    private var image: String? = null
    private var windowWidth = 0
    private var scaleRatio = 1f
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var binding: FragmentImageViewBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
      /*  sharedElementEnterTransition = MaterialContainerTransform().apply {
            scrimColor = Color.TRANSPARENT
        }
        exitTransition = MaterialFadeThrough()
        postponeEnterTransition()*/

        binding = FragmentImageViewBinding.inflate(inflater)

     /*   params = Pair(arguments?.getInt(ARG_WIDTH) ?: 0, arguments?.getInt(ARG_HEIGHT) ?: 0)

        scaleRatio = (params.first/params.second).toFloat()
        windowWidth = getWindowWidth()

        binding.fullscreenImage.updateLayout(height = convertDpToPx(params.second), width = windowWidth)
        transitionName = arguments?.getString(ARG_TRANSITION_NAME)*/
        image = arguments?.getString(ARG_IMAGE)

//        binding.fullscreenImage.transitionName = transitionName

        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d(TAG, image.toString())

        binding.imageViewFragmentToolbar.setNavigationOnClickListener {
//            activity?.supportFragmentManager?.beginTransaction()?.remove(this)?.commit()
            findNavController().navigateUp()
        }

        binding.fullscreenImage.apply {
          /*  setAllowTouchInterceptionWhileZoomed(false)
            setIsLongpressEnabled(false)
            setTapListener(TapListener(this))*/
            setTapListener(TapListener(this))

            val imageRequest = ImageRequest.fromUri(image)

            val controller = Fresco.newDraweeControllerBuilder()
                .setImageRequest(imageRequest)
                .setCallerContext(this)
                .build()

            setController(controller)

//            startPostponedEnterTransition()
        }

//        binding.fullscreenImage.setImageURI(image?.toUri())

//        binding.fullscreenImage.setScaleListener(this)

        binding.fullscreenImage.setOnClickListener {
            val nightModeFlags = requireActivity().resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK
            if (binding.imageViewFragmentAppBar.translationY == 0f) {

                when (nightModeFlags) {
                    Configuration.UI_MODE_NIGHT_YES -> {

                    }
                    Configuration.UI_MODE_NIGHT_NO -> {
                        binding.fullscreenImage.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.black))
                    }
                    Configuration.UI_MODE_NIGHT_UNDEFINED -> {

                    }
                }
                hideTopAndBottomActions(binding.imageViewFragmentAppBar)
            } else {
                when (nightModeFlags) {
                    Configuration.UI_MODE_NIGHT_YES -> {

                    }
                    Configuration.UI_MODE_NIGHT_NO -> {
                        binding.fullscreenImage.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorGrey))
                    }
                    Configuration.UI_MODE_NIGHT_UNDEFINED -> {

                    }
                }
                showTopAndBottomActions(binding.imageViewFragmentAppBar)
            }
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, _) ->
            binding.imageViewFragmentToolbar.updateLayout(marginTop = top)
        }
    }

    private fun hideTopAndBottomActions(top: View) {
        val animator = ObjectAnimator.ofFloat(top, View.TRANSLATION_Y, -top.measuredHeight.toFloat())

        AnimatorSet().apply {
            duration = 250
            interpolator = AccelerateDecelerateInterpolator()
            playTogether(animator)
            start()
        }
    }

    private fun showTopAndBottomActions(top: View) {
        val animator = ObjectAnimator.ofFloat(top, View.TRANSLATION_Y, 0f)

        AnimatorSet().apply {
            duration = 250
            interpolator = AccelerateDecelerateInterpolator()
            playTogether(animator)
            start()
        }
    }

    companion object {

        const val ARG_TRANSITION_NAME = "ARG_TRANSITION_NAME"
        const val ARG_IMAGE = "ARG_IMAGE"
        const val ARG_WIDTH = "ARG_WIDTH"
        const val ARG_HEIGHT = "ARG_HEIGHT"
        const val TAG = "ImageViewFragment"

        @JvmStatic
        fun newInstance(image: Pair<String, String>? = null, width: Int = 0, height: Int = 0)
            = ImageViewFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TRANSITION_NAME, image?.first)
                    putString(ARG_IMAGE, image?.second)
                    putInt(ARG_WIDTH, width)
                    putInt(ARG_HEIGHT, height)
                }
            }
    }

    override fun onImageChange(scaleFactor: Float) {
        Log.d(TAG, scaleFactor.toString())
        if (scaleFactor <= 1f) {
            binding.fullscreenImage.updateLayout(convertDpToPx(params.second), windowWidth)
        } else {
            binding.fullscreenImage.updateLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }

}