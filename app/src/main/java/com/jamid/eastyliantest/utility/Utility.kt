package com.jamid.eastyliantest.utility

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.*
import android.net.Uri
import android.text.format.DateUtils
import android.util.Patterns
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.setMargins
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.navOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.GetTokenResult
import com.jamid.eastyliantest.*
import com.jamid.eastyliantest.model.Flavor
import com.jamid.eastyliantest.model.OrderStatus
import com.jamid.eastyliantest.ui.AdminActivity
import com.jamid.eastyliantest.ui.DeliveryActivity
import com.jamid.eastyliantest.ui.MainActivity
import java.util.*

fun Context.convertDpToPx(dp: Int) = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    dp.toFloat(),
    this.resources.displayMetrics
).toInt()

fun Fragment.convertDpToPx(dp: Int) = requireContext().convertDpToPx(dp)

//fun getWindowHeight() = Resources.getSystem().displayMetrics.heightPixels

//fun getWindowWidth() = Resources.getSystem().displayMetrics.widthPixels

fun getTextForTime(time: Long): String {
    return DateUtils.getRelativeTimeSpanString(time, Calendar.getInstance().timeInMillis, DateUtils.MINUTE_IN_MILLIS).toString()
}

fun Fragment.getImageResourceBasedOnFlavor(flavor: Flavor, name: String = ""): Int {
    return requireContext().getImageResourceBasedOnFlavor(flavor, name)
}

// if the cake is not customizable then get an image from assets
fun Context.getImageResourceBasedOnFlavor(flavor: Flavor, name: String = getString(R.string.fondant)): Int {
    return when (flavor) {
        Flavor.BLACK_FOREST -> R.drawable.black_forest
        Flavor.WHITE_FOREST -> R.drawable.white_forest
        Flavor.VANILLA -> R.drawable.vanilla
        Flavor.CHOCOLATE_FANTASY -> R.drawable.chocolate_fantasy
        Flavor.RED_VELVET -> R.drawable.red_velvet
        Flavor.HAZELNUT -> R.drawable.hazelnut
        Flavor.MANGO -> R.drawable.mango
        Flavor.STRAWBERRY -> R.drawable.strawberry
        Flavor.KIWI -> R.drawable.kiwi
        Flavor.ORANGE -> R.drawable.orange
        Flavor.PINEAPPLE -> R.drawable.pineapple
        Flavor.BUTTERSCOTCH -> R.drawable.butterscotch
        Flavor.NONE -> getImageResourceBasedOnBaseName(name)
    }
}

fun CharSequence?.isValidEmail() = !isNullOrEmpty() && Patterns.EMAIL_ADDRESS.matcher(this).matches()

fun Fragment.getImageResourceBasedOnBaseName(name: String): Int {
    return requireContext().getImageResourceBasedOnBaseName(name)
}

fun Context.getImageResourceBasedOnBaseName(name: String): Int {
    return if (name == getString(R.string.fondant)) {
        R.drawable.fondant
    } else {
        R.drawable.sponge
    }
}

fun Context.composeEmail(subject: String, vararg addresses: String) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        type = "*/*"
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, addresses)
        putExtra(Intent.EXTRA_SUBJECT, subject)
    }
    if (intent.resolveActivity(packageManager) != null) {
        startActivity(intent)
    } else {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        val clip = ClipData.newPlainText("label", addresses.first())
        clipboard?.setPrimaryClip(clip)
        toast("Text copied")
    }
}

fun View.updateLayout(
    height: Int? = null,
    width: Int? = null,
    margin: Int? = null,
    marginLeft: Int? = null,
    marginTop: Int? = null,
    marginRight: Int? = null,
    marginBottom: Int? = null,
    padding: Int? = null,
    paddingLeft: Int? = null,
    paddingTop: Int? = null,
    paddingRight: Int? = null,
    paddingBottom: Int? = null,
    ignoreParams: Boolean? = true,
    ignoreMargin: Boolean? = true,
    ignorePadding: Boolean? = true,
    extras: Map<String, Int>? = null) {

    var ilp = ignoreParams
    var im = ignoreMargin
    var ip = ignorePadding

    if (width != null || height != null) {
        ilp = false
    }

    if (margin != null || marginLeft != null || marginTop != null || marginRight != null || marginBottom != null) {
        im = false
    }

    if (padding != null || paddingLeft != null || paddingTop != null || paddingRight != null || paddingBottom != null) {
        ip = false
    }

    if (ilp != null && !ilp) {
        val params = if (extras != null) {
            val p1 = this.layoutParams as ConstraintLayout.LayoutParams
            p1.height = height ?: ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
            p1.width = width ?: ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
            val defaultId = (this.parent as ConstraintLayout).id
            p1.apply {
                startToStart = extras[START_TO_START] ?: defaultId
                endToEnd = extras[END_TO_END] ?: defaultId
                topToTop = extras[TOP_TO_TOP] ?: defaultId
                bottomToBottom = extras[BOTTOM_TO_BOTTOM] ?: defaultId
                if (extras.containsKey(START_TO_END)) {
                    startToEnd = extras[START_TO_END]!!
                }
                if (extras.containsKey(END_TO_START)) {
                    endToStart = extras[END_TO_START]!!
                }
                if (extras.containsKey(TOP_TO_BOTTOM)) {
                    topToBottom = extras[TOP_TO_BOTTOM]!!
                }
                if (extras.containsKey(BOTTOM_TO_TOP)) {
                    bottomToTop = extras[BOTTOM_TO_TOP]!!
                }
            }
            p1
        } else {
            val p1 = this.layoutParams as ViewGroup.LayoutParams
            p1.height = height ?: ViewGroup.LayoutParams.WRAP_CONTENT
            p1.width = width ?: ViewGroup.LayoutParams.MATCH_PARENT
            p1
        }

        this.layoutParams = params
    }

    if (im != null && !im) {
        val marginParams = this.layoutParams as ViewGroup.MarginLayoutParams
        if (margin != null) {
            marginParams.setMargins(margin)
        } else {
            marginParams.setMargins(marginLeft ?: 0, marginTop ?: 0, marginRight ?: 0, marginBottom ?: 0)
        }
        this.requestLayout()
    }

    if (ip != null && !ip) {
        if (padding != null) {
            this.setPadding(padding)
        } else {
            this.setPadding(paddingLeft ?: 0, paddingTop ?: 0, paddingRight ?: 0, paddingBottom ?: 0)
        }
    }
}

/*fun Activity.getFullScreenHeight(): Int {
    return if (Build.VERSION.SDK_INT > 29) {
        val rect = windowManager.maximumWindowMetrics.bounds
        rect.bottom - rect.top
    } else {
        getWindowHeight()
    }
}*/

/*fun Fragment.getFullScreenHeight(): Int {
    return requireActivity().getFullScreenHeight()
}*/

fun Fragment.hideKeyboard() {
    view?.let { activity?.hideKeyboard(it) }
}

/**
 * Extension function to hide the keyboard from the given context
 *
 * @param view
 */
fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun slideRightNavOptions(): NavOptions {
    return navOptions {
        anim {
            enter = R.anim.slide_in_right
            exit = R.anim.slide_out_left
            popEnter = R.anim.slide_in_left
            popExit = R.anim.slide_out_right
        }
    }
}

fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.GONE
}

fun View.disappear() {
    visibility = View.INVISIBLE
}

fun ViewGroup.show() {
    visibility = View.VISIBLE
}

fun ViewGroup.hide() {
    visibility = View.GONE
}

/*fun ViewGroup.disappear() {
    visibility = View.INVISIBLE
}*/

fun View.enable() {
    isEnabled = true
}

fun View.disable() {
    isEnabled = false
}

fun ViewGroup.enable() {
    isEnabled = true
}

fun ViewGroup.disable() {
    isEnabled = false
}

fun Fragment.startActivityBasedOnAuth(result: GetTokenResult) {
    requireContext().startActivityBasedOnAuth(result)
}

fun Context.startActivityBasedOnAuth(result: GetTokenResult) {
    if (result.claims.containsKey(ADMIN)) {
        val isAdmin = result.claims[ADMIN] as Boolean?
        val intent = if (isAdmin != null && isAdmin) {
            val level = result.claims["level"] as Int?
            if (level != null) {
                if (level == 0) {
                    Intent(this, AdminActivity::class.java)
                } else {
                    Intent(this, DeliveryActivity::class.java)
                }
            } else {
                Intent(this, DeliveryActivity::class.java)
            }
        } else {
            Intent(this, MainActivity::class.java)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        (this as Activity).finish()
    } else {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        (this as Activity).finish()
    }
}

fun randomId(): String {
    return UUID.randomUUID().toString().replace("-", "")
}


fun View.slideReset() {
    val animator = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, 0f)
    animator.duration = 300
    animator.interpolator = AccelerateDecelerateInterpolator()
    animator.start()
}

/*fun View.slideUp(offset: Float) {
    val animator = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, -offset)
    animator.duration = 300
    animator.interpolator = AccelerateDecelerateInterpolator()
    animator.start()
}*/

fun View.slideDown(offset: Float) {
    val animator = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, offset)
    animator.duration = 300
    animator.interpolator = AccelerateDecelerateInterpolator()
    animator.start()
}

/*fun Int.fromHourToMilliseconds(): Int {
    return (this * 60).fromMinuteToMilliseconds()
}*/

/*fun Int.fromMinuteToMilliseconds(): Int {
    return (this * 60).fromSecondToMilliseconds()
}*/

/*fun Int.fromSecondToMilliseconds(): Int {
    return this * 1000
}*/

fun String.toOrderStatus(): OrderStatus {
    return when (this) {
        CREATED -> OrderStatus.Created
        "Paid" -> OrderStatus.Paid
        "Preparing" -> OrderStatus.Preparing
        "Delivering" -> OrderStatus.Delivering
        "Delivered" -> OrderStatus.Delivered
        "Cancelled" -> OrderStatus.Cancelled
        "Rejected" -> OrderStatus.Rejected
        "Due" -> OrderStatus.Due
        else -> OrderStatus.Created
    }
}

fun Fragment.showDialog(title: String? = null, message: String? = null, positiveBtn: String? = null, negativeBtn: String? = null, onPositiveBtnClick: ((a: DialogInterface) -> Unit)? = null, onNegativeBtnClick: ((d: DialogInterface) -> Unit)? = null, extraView: View? = null): AlertDialog {
    return requireContext().showDialog(title, message, positiveBtn, negativeBtn, onPositiveBtnClick, onNegativeBtnClick, extraView)
}

fun Context.showDialog(title: String? = null, message: String? = null, positiveBtn: String? = null, negativeBtn: String? = null, onPositiveBtnClick: ((d: DialogInterface) -> Unit)? = null, onNegativeBtnClick: ((d: DialogInterface) -> Unit)? = null, extraView: View? = null): AlertDialog {

    val d = MaterialAlertDialogBuilder(this)

    if (title != null) {
        d.setTitle(title)
    }

    if (message != null) {
        d.setMessage(message)
    }

    if (positiveBtn != null) {
        d.setPositiveButton(positiveBtn) { a, _ ->
            onPositiveBtnClick?.let { it(a) }
        }
    }

    if (negativeBtn != null) {
        d.setNegativeButton(negativeBtn) { a, _ ->
            onNegativeBtnClick?.let { it(a) }
        }
    }

    if (extraView != null) {
        d.setView(extraView)
    }

    return d.show()
}

fun <T: Activity> Fragment.toActivity(clazz: Class<T>) {
    requireContext().toActivity(clazz)
}

fun <T: Activity> Context.toActivity(clazz: Class<T>) {
    val intent = Intent(this, clazz)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    startActivity(intent)
    (this as Activity).finish()
}

fun Fragment.toast(msg: String) {
    requireContext().toast(msg)
}

fun Context.toast(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

fun Fragment.getFlavorName(flavor: Flavor): String {
    return requireContext().getFlavorName(flavor)
}

fun Context.getFlavorName(flavor: Flavor): String {
    return when (flavor) {
        Flavor.BLACK_FOREST -> getString(R.string.black_forest)
        Flavor.WHITE_FOREST -> getString(R.string.white_forest)
        Flavor.VANILLA -> getString(R.string.vanilla)
        Flavor.CHOCOLATE_FANTASY -> getString(R.string.chocolate_fantasy)
        Flavor.RED_VELVET -> getString(R.string.red_velvet)
        Flavor.HAZELNUT -> getString(R.string.hazelnut)
        Flavor.MANGO -> getString(R.string.mango)
        Flavor.STRAWBERRY -> getString(R.string.strawberry)
        Flavor.KIWI -> getString(R.string.kiwi)
        Flavor.ORANGE -> getString(R.string.orange)
        Flavor.PINEAPPLE -> getString(R.string.pineapple)
        Flavor.BUTTERSCOTCH -> getString(R.string.butterscotch)
        Flavor.NONE -> ""
    }
}


//const val utilityTag = "UtilityTag"