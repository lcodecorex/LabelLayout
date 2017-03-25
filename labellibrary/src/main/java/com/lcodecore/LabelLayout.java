package com.lcodecore;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.lcodecore.labellibrary.R;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A LabelLayout ViewGroup that can hold labels with the help of CheckBox.
 * 配合CheckBox使用,可设置允许的最大标签选择数量
 */
public class LabelLayout extends ViewGroup {

    private int mMaxCheckCount = Integer.MAX_VALUE;
    /**
     * 竖直方向间距, default is 8.0dp.
     */
    private int horizontalSpacing;

    /**
     * 水平方向间距, default is 4.0dp.
     */
    private int verticalSpacing;

    //whether or not to draw the divider between labels at horizon.
    private boolean enableDivider = false; //是否允许显示分割线  默认不显示
    private int dividerColor = 0xffECECEC;
    private float dividerHeight;

    private int checkboxLayoutId;

    //nark checked labels.
    private Map<String, Boolean> labelcheckMap;
    //mark the first position in a row.
    private Set<Integer> rowPositons = new HashSet<>();

    //The paint to draw the divider.
    Paint dividerPaint;

    public LabelLayout(Context context) {
        this(context, null);
    }

    public LabelLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LabelLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWillNotDraw(false);

        labelcheckMap = new HashMap<>();

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LabelLayout, defStyleAttr, R.style.LabelLayoutDefault);
        try {
            horizontalSpacing = (int) a.getDimension(R.styleable.LabelLayout_label_horizontalSpacing, dp2px(8.0f));
            verticalSpacing = (int) a.getDimension(R.styleable.LabelLayout_label_verticalSpacing, dp2px(4.0f));

            checkboxLayoutId = a.getResourceId(R.styleable.LabelLayout_label_checkboxLayout, R.layout.view_label_common);
            enableDivider = a.getBoolean(R.styleable.LabelLayout_label_enableDivider, false);
            dividerHeight = a.getDimension(R.styleable.LabelLayout_label_dividerHeight, dp2px(2));
            dividerColor = a.getColor(R.styleable.LabelLayout_label_dividerColor, 0xffECECEC);
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (enableDivider) {
            if (dividerPaint == null) {
                dividerPaint = new Paint();
                dividerPaint.setAntiAlias(true);
                dividerPaint.setColor(dividerColor);
                dividerPaint.setStyle(Paint.Style.FILL);
            }

            for (Integer top : rowPositons) {
                if (top != 0) {
                    //draw lines between labels.
                    canvas.drawRect(0, top - dividerHeight / 2, getMeasuredWidth(), top + dividerHeight / 2, dividerPaint);
                }
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        measureChildren(widthMeasureSpec, heightMeasureSpec);

        int width = 0;
        int height = 0;

        int row = 0; // The row counter.
        int rowWidth = 0; // Calc the current row width.
        int rowMaxHeight = 0; // Calc the max tag height, in current row.

        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            final int childWidth = child.getMeasuredWidth();
            final int childHeight = child.getMeasuredHeight();

            if (child.getVisibility() != GONE) {
                rowWidth += childWidth;
                if (rowWidth > widthSize) { // Next line.
                    rowWidth = childWidth; // The next row width.
                    height += rowMaxHeight + verticalSpacing;
                    rowMaxHeight = childHeight; // The next row max height.
                    row++;
                } else { // This line.
                    rowMaxHeight = Math.max(rowMaxHeight, childHeight);
                }
                rowWidth += horizontalSpacing;
            }
//            System.out.println("measured height:" + height);
            rowPositons.add(height - verticalSpacing / 2);
        }
        // Account for the last row height.
        height += rowMaxHeight;

        // Account for the padding too.
        height += getPaddingTop() + getPaddingBottom();

        // If the tags grouped in one row, set the width to wrap the tags.
        if (row == 0) {
            width = rowWidth;
            width += getPaddingLeft() + getPaddingRight();
        } else {// If the tags grouped exceed one line, set the width to match the parent.
            width = widthSize;
        }

        setMeasuredDimension(widthMode == MeasureSpec.EXACTLY ? widthSize : width,
                heightMode == MeasureSpec.EXACTLY ? heightSize : height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int parentLeft = getPaddingLeft();
        final int parentRight = r - l - getPaddingRight();
        final int parentTop = getPaddingTop();
        final int parentBottom = b - t - getPaddingBottom();

        int childLeft = parentLeft;
        int childTop = parentTop;

        int rowMaxHeight = 0;

        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            final int width = child.getMeasuredWidth();
            final int height = child.getMeasuredHeight();

            if (child.getVisibility() != GONE) {
                if (childLeft + width > parentRight) { // Next line
                    childLeft = parentLeft;
                    childTop += rowMaxHeight + verticalSpacing;
                    rowMaxHeight = height;
                } else {
                    rowMaxHeight = Math.max(rowMaxHeight, height);
                }
                child.layout(childLeft, childTop, childLeft + width, childTop + height);

                childLeft += width + horizontalSpacing;
            }
        }
    }

    /**
     * set the default labels that you wanna to add into LabelLayout.
     *
     * @param labels A collection contains objects that implement ILabel interface.
     */
    public void setLabels(List<ILabel> labels) {
        labelcheckMap.clear();
        removeAllViews();
        if (labels == null || labels.size() == 0) return;
        for (final ILabel label : labels) {
            final CheckBox tagView = (CheckBox) View.inflate(getContext(), checkboxLayoutId, null);
            tagView.setText(label.getName());
            addView(tagView);
            tagView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {//选中时添加到map
                        if (labelcheckMap.size() > mMaxCheckCount - 1) {
                            if (checkListener != null) {
                                checkListener.onBeyondMaxCheckCount();
                            }
                            tagView.setChecked(false);
                        } else {
                            labelcheckMap.put(label.getId(), true);
                            if (checkListener != null) {
                                checkListener.onCheckChanged(label, true);
                            }
                        }
                    } else {//否则及时清理map
                        if (labelcheckMap.containsKey(label.getId())) {
                            labelcheckMap.remove(label.getId());
                            if (checkListener != null) {
                                checkListener.onCheckChanged(label, false);
                            }
                        }
                    }
                }
            });
        }
    }

    /**
     * set the maximum numbers of checked labels.
     */
    public void setMaxCheckCount(int count) {
        mMaxCheckCount = count;
    }

    /**
     * Get the current selected tag number.
     */
    public int getCheckedLabelsCount() {
        int count = 0;
        for (Map.Entry<String, Boolean> m : labelcheckMap.entrySet()) {
            if (m.getValue()) {
                count++;
            }
        }
        return count;
    }

    public List<String> getCheckedLabelIds() {
        List<String> chechedLabelIds = new ArrayList<>();
        for (Map.Entry<String, Boolean> m : labelcheckMap.entrySet()) {
            if (m.getValue()) {
                chechedLabelIds.add(m.getKey());
            }
        }
        return chechedLabelIds;
    }

    /**
     * To serialize checked-label ids as json, make benefit for use.
     *
     * @return json string
     */
    public String getCheckedIdsAsJson() {
        List<String> chechedId = new ArrayList<>();
        for (Map.Entry<String, Boolean> m : labelcheckMap.entrySet()) {
            if (m.getValue()) {
                chechedId.add(m.getKey());
            }
        }
        return new JSONArray(chechedId).toString();
    }


    private float dp2px(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }

    private OnCheckChangeListener checkListener;

    public interface OnCheckChangeListener {
        void onCheckChanged(ILabel label, boolean isChecked);

        void onBeyondMaxCheckCount();
    }

    public void setOnCheckChangedListener(OnCheckChangeListener checkListener) {
        this.checkListener = checkListener;
    }

}
