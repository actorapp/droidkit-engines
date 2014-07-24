package com.droidkit.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

import com.droidkit.core.Logger;

public class BlockingListView extends ListView {

    private boolean mBlockLayoutChildren;

    public BlockingListView(Context context) {
        super(context);
    }

    public BlockingListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BlockingListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    public void setBlockLayoutChildren(boolean block) {
        mBlockLayoutChildren = block;
    }

    @Override
    protected void layoutChildren() {
        if (!mBlockLayoutChildren) {
            try {
                super.layoutChildren();
            } catch (Exception e) {
                Logger.e(e);
            }
        }
    }
}