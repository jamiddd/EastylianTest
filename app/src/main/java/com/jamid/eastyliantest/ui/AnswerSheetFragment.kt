package com.jamid.eastyliantest.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.databinding.AnswerSheetBottomBinding
import com.jamid.eastyliantest.model.Faq

class AnswerSheetFragment: BottomSheetDialogFragment() {

	private lateinit var binding: AnswerSheetBottomBinding
	private val viewModel: MainViewModel by activityViewModels()

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		return inflater.inflate(R.layout.answer_sheet_bottom, container, false)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setStyle(DialogFragment.STYLE_NORMAL, R.style.DialogStyle)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding = AnswerSheetBottomBinding.bind(view)

		val faq = arguments?.getParcelable<Faq>(ARG_FAQ) ?: return

		val question = "Q. " + faq.question
		binding.questionText.text = question

		binding.answerBtn.setOnClickListener {
			Firebase.firestore.collection("faq")
				.document(faq.id)
				.update(mapOf("answer" to binding.answerText.text.toString(), "answered" to true))
				.addOnSuccessListener {
					Toast.makeText(requireContext(), "Uploaded answer.", Toast.LENGTH_SHORT).show()
					dismiss()
				}.addOnFailureListener {
					viewModel.setCurrentError(it)
				}
		}

		binding.cancelBtn.setOnClickListener {
			dismiss()
		}

	}

	companion object {
		const val ARG_FAQ = "ARG_FAQ"
		const val TAG = "AnswerSheetFragment"

		@JvmStatic
		fun newInstance(faq: Faq) = AnswerSheetFragment().apply {
			arguments = Bundle().apply {
				putParcelable(ARG_FAQ, faq)
			}
		}

	}

}