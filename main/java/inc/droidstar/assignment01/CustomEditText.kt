package inc.droidstar.assignment01

import android.graphics.RectF
import android.text.TextUtils
import android.text.TextPaint
import android.os.Build
import android.annotation.TargetApi
import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.util.TypedValue
import android.util.SparseIntArray
import androidx.annotation.RequiresApi
import android.widget.EditText


class CustomEditText : EditText {

    private val mTextRect = RectF()

    private var mAvailableSpaceRect: RectF? = null

    private var mTextCachedSizes: SparseIntArray? = null

    private var mPaint: TextPaint? = null

    private var mMaxTextSize: Float = 0.toFloat()

    private var mSpacingMult = 1.0f

    private var mSpacingAdd = 0.0f

    private var mMinTextSize = 16f

    private var mWidthLimit: Int = 0
    private var mMaxLines: Int = 0

    private val mEnableSizeCache = true
    private var mInitializedDimens: Boolean = false

    var sizeTester: SizeTester = object : SizeTester {
        internal val textRect = RectF()

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        override fun onTestSize(
            suggestedSize: Int,
            availableSpace: RectF
        ): Int {

            val paint = TextPaint()

            paint.textSize = suggestedSize.toFloat()

            val text: String
            if (!TextUtils.isEmpty(hint)) {
                text = hint.toString()
            } else {
                text = getText().toString()
            }

            textRect.bottom = paint.fontSpacing
            textRect.right = paint.measureText(text)
            textRect.offsetTo(0f, 0f)

            return if (availableSpace.contains(textRect)) {
                -1
            } else {
                1
            }
        }
    }

    constructor(context: Context) : super(context) {
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initialize()
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    ) {
        initialize()
    }

    interface SizeTester {
        fun onTestSize(suggestedSize: Int, availableSpace: RectF): Int
    }

    private fun initialize() {
        mPaint = TextPaint(paint)
        mMaxTextSize = textSize
        mAvailableSpaceRect = RectF()
        mTextCachedSizes = SparseIntArray()
        if (mMaxLines == 0) {
            // no value was assigned during construction
            mMaxLines = NO_LINE_LIMIT
        }
    }

    override fun setTextSize(size: Float) {
        mMaxTextSize = size
        mTextCachedSizes!!.clear()
        adjustTextSize(text.toString())
    }

    override fun setMaxLines(maxlines: Int) {
        super.setMaxLines(maxlines)
        mMaxLines = maxlines
        adjustTextSize(text.toString())
    }

    override fun getMaxLines(): Int {
        return mMaxLines
    }

    override fun setSingleLine() {
        super.setSingleLine()
        mMaxLines = 1
        adjustTextSize(text.toString())
    }

    override fun setSingleLine(singleLine: Boolean) {
        super.setSingleLine(singleLine)
        if (singleLine) {
            mMaxLines = 1
        } else {
            mMaxLines = NO_LINE_LIMIT
        }
        adjustTextSize(text.toString())
    }

    override fun setLines(lines: Int) {
        super.setLines(lines)
        mMaxLines = lines
        adjustTextSize(text.toString())
    }

    override fun setTextSize(unit: Int, size: Float) {
        val c = context
        val r: Resources

        if (c == null)
            r = Resources.getSystem()
        else
            r = c.resources
        mMaxTextSize = TypedValue.applyDimension(
            unit, size,
            r.getDisplayMetrics()
        )
        mTextCachedSizes!!.clear()
        adjustTextSize(text.toString())
    }

    override fun setLineSpacing(add: Float, mult: Float) {
        super.setLineSpacing(add, mult)
        mSpacingMult = mult
        mSpacingAdd = add
    }

    fun setMinTextSize(minTextSize: Float) {
        mMinTextSize = minTextSize
        adjustTextSize(text.toString())
    }

    private fun adjustTextSize(s: String) {
        if (!mInitializedDimens) {
            return
        }
        val startSize = mMinTextSize.toInt()
        val heightLimit = (measuredHeight - compoundPaddingBottom
                - compoundPaddingTop)
        mWidthLimit = (measuredWidth - compoundPaddingLeft
                - compoundPaddingRight)
        mAvailableSpaceRect!!.right = mWidthLimit.toFloat()
        mAvailableSpaceRect!!.bottom = heightLimit.toFloat()
        super.setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            efficientTextSizeSearch(
                s, startSize, mMaxTextSize.toInt(),
                sizeTester, mAvailableSpaceRect!!
            ).toFloat()
        )
    }

    private fun efficientTextSizeSearch(
        s: CharSequence, start: Int, end: Int,
        sizeTester: SizeTester, availableSpace: RectF
    ): Int {
        if (!mEnableSizeCache) {
            return binarySearch(s, start, end, sizeTester, availableSpace)
        }
        val key = text.toString().length
        var size = mTextCachedSizes!!.get(key)
        if (size != 0) {
            return size
        }
        size = binarySearch(s, start, end, sizeTester, availableSpace)
        mTextCachedSizes!!.put(key, size)
        return size
    }

    override fun onTextChanged(
        text: CharSequence, start: Int,
        before: Int, after: Int
    ) {
        super.onTextChanged(text, start, before, after)
        adjustTextSize(getText().toString())
    }

    override fun onSizeChanged(
        width: Int, height: Int, oldwidth: Int,
        oldheight: Int
    ) {
        mInitializedDimens = true
        mTextCachedSizes!!.clear()
        super.onSizeChanged(width, height, oldwidth, oldheight)
        if (width != oldwidth || height != oldheight) {
            adjustTextSize(text.toString())
        }
    }

    companion object {

        private val NO_LINE_LIMIT = -1

        private fun binarySearch(
            s: CharSequence, start: Int, end: Int, sizeTester: SizeTester,
            availableSpace: RectF
        ): Int {
            var lastBest = start
            var lo = start
            var hi = end - 1
            var mid = 0
            while (lo <= hi) {
                mid = (lo + hi).ushr(1)
                val midValCmp = sizeTester.onTestSize(mid, availableSpace)
                if (midValCmp < 0) {
                    lastBest = lo
                    lo = mid + 1
                } else if (midValCmp > 0) {
                    hi = mid - 1
                    lastBest = hi
                } else {
                    return mid
                }
            }
            return lastBest

        }
    }
}