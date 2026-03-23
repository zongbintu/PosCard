package com.tu.poscard.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.tu.poscard.R;
import com.tu.poscard.data.model.WrapSettleType;
import com.tu.poscard.util.Utils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * 结算方式RecyclerViewAdapter
 */
public class SettleTypeAdapter extends RecyclerView.Adapter<SettleTypeAdapter.ViewHolder> {


    private final List<WrapSettleType> mValues;
    OnSettleTypeListener onSettleTypeListener;

    public SettleTypeAdapter(List<WrapSettleType> items, OnSettleTypeListener onSettleTypeListener) {
        mValues = items;
        this.onSettleTypeListener = onSettleTypeListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_settle, parent, false));

    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final WrapSettleType data = mValues.get(position);
        holder.nameTextView.setText(data.getName());
        holder.expressionTextView.setText(Utils.settleExpression(data));
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onSettleTypeListener != null) {
                    onSettleTypeListener.settleTypeChecked(data);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public View itemView;
        @BindView(R.id.name_tv)
        TextView nameTextView;
        @BindView(R.id.expression_tv)
        TextView expressionTextView;

        public ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            itemView = view;
        }
    }

    interface OnSettleTypeListener {
        void settleTypeChecked(WrapSettleType settleType);
    }
}
