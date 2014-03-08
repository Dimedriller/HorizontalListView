package com.dimedriller.alternativeui.listview;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

public class AbsListState extends View.BaseSavedState {
    public static final Creator<AbsListState> CREATOR = new Creator<AbsListState>() {
        @Override
        public AbsListState createFromParcel(Parcel parcel) {
            return new AbsListState(parcel);
        }

        @Override
        public AbsListState[] newArray(int count) {
            return new AbsListState[count];
        }
    };

    private final int mFirstItemIndex;
    private final int mFirstItemOffset;

    public AbsListState(Parcelable parcelable, int firstItemIndex, int firstItemOffset) {
        super(parcelable);

        mFirstItemIndex = firstItemIndex;
        mFirstItemOffset = firstItemOffset;
    }

    public AbsListState() {
        super(View.BaseSavedState.EMPTY_STATE);

        mFirstItemIndex = 0;
        mFirstItemOffset = 0;
    }

    public AbsListState(Parcel source) {
        super(source);
        mFirstItemIndex = source.readInt();
        mFirstItemOffset = source.readInt();
    }

    public int getFirstItemIndex() {
        return mFirstItemIndex;
    }

    public int getFirstItemOffset() {
        return mFirstItemOffset;
    }

    @Override
    public int describeContents() {
        return super.describeContents();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);

        dest.writeInt(mFirstItemIndex);
        dest.writeInt(mFirstItemOffset);
    }
}
