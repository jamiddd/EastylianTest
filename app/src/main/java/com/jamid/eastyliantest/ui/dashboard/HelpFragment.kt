package com.jamid.eastyliantest.ui.dashboard

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.adapter.FaqAdapter
import com.jamid.eastyliantest.databinding.FragmentHelpBinding
import com.jamid.eastyliantest.model.Faq
import com.jamid.eastyliantest.ui.MainViewModel
import com.jamid.eastyliantest.utility.disable
import com.jamid.eastyliantest.utility.hide
import com.jamid.eastyliantest.utility.show
import com.jamid.eastyliantest.utility.slideRightNavOptions

class HelpFragment: Fragment(R.layout.fragment_help) {

	private lateinit var binding: FragmentHelpBinding
	private lateinit var faqAdapter: FaqAdapter
	private val viewModel: MainViewModel by activityViewModels()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setHasOptionsMenu(true)
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		super.onCreateOptionsMenu(menu, inflater)
		inflater.inflate(R.menu.help_menu, menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			R.id.contact -> {
				findNavController().navigate(R.id.action_helpFragment2_to_contactFragment3, null, slideRightNavOptions())
			}
		}
		return super.onOptionsItemSelected(item)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding = FragmentHelpBinding.bind(view)

		faqAdapter = FaqAdapter()

		binding.faqRecycler.apply {
			layoutManager = LinearLayoutManager(requireContext())
			adapter = faqAdapter
			addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
		}

		binding.problemTextLayout.editText?.doAfterTextChanged {
			binding.submitBtn.isEnabled = !it.isNullOrBlank() && it.length > 10
		}

		binding.submitBtn.setOnClickListener {
			binding.submitBtn.disable()

			val question = binding.problemTextLayout.editText?.text.toString()
			viewModel.sendQuestion(question)

			binding.problemSubmittedHeader.show()
			binding.problemSubmittedMessage.show()

			binding.submitBtn.hide()
			binding.problemTextLayout.hide()
		}

		getQuestions()

		viewModel.repo.allFaqs.observe(viewLifecycleOwner) {
			if (it.isNullOrEmpty()) {
				binding.faqRecycler.hide()
				binding.textView30.hide()
			} else {
				binding.faqRecycler.show()
				binding.textView30.show()
				faqAdapter.submitList(it)
			}
		}

		/*binding.contactBtn.setOnClickListener {
			findNavController().navigate(R.id.action_helpFragment_to_contactFragment2, null, slideRightNavOptions())
		}*/
	}

	private fun getQuestions() {
		Log.d(TAG, "Getting questions ..")
		Firebase.firestore.collection("faq")
			.whereEqualTo("answered", true)
			.orderBy("rating", Query.Direction.DESCENDING)
			.limit(10)
			.get()
			.addOnSuccessListener { it1 ->
				if (!it1.isEmpty) {

					val faqs = it1.toObjects(Faq::class.java)

					Log.d(TAG, "Got some --- ${faqs.size}")

					val answeredFaqs = faqs.filter { faq ->
						faq.answered
					}

					if (answeredFaqs.isNotEmpty()) {
						viewModel.insertFaqs(faqs)
					} else {
						binding.faqRecycler.hide()
						binding.textView30.hide()
					}
				} else {

					Log.d(TAG, "No such questions found")

					binding.faqRecycler.hide()
					binding.textView30.hide()
				}
			}.addOnFailureListener { it1 ->
				Log.d(TAG, it1.localizedMessage!!)
				viewModel.setCurrentError(it1)
			}
	}

	companion object {
		private const val TAG = "HelpFragment"
	}

}