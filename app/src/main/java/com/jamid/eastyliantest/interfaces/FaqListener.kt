package com.jamid.eastyliantest.interfaces

import com.jamid.eastyliantest.model.Faq

interface FaqListener {
	fun onAnswerClick(faq: Faq)
	fun onReviewClick(faq: Faq)
}