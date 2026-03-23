package com.tu.poscard.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.tu.poscard.R;
import com.tu.poscard.data.model.Sold;
import com.tu.poscard.util.MathUtils;
import com.tu.poscard.util.Utils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;


public class SoldAdapter extends RecyclerView.Adapter<SoldAdapter.ViewHolder> {
    private static final int ITEM_TYPE_HEADER = 1;
    private static final int ITEM_TYPE_NORMAL = 2;
    private final List<Sold> mValues;
    OnDatePickerClickedListener onDatePickerClickedListener;
    private String sum = "";
    private String month = "";

    public SoldAdapter(OnDatePickerClickedListener listener, List<Sold> items) {
        mValues = items;
        this.onDatePickerClickedListener = listener;
    }

    /**
     * 更新汇总数据
     *
     * @param sum 合计
     */
    public void updateSum(String sum) {

        this.sum = sum;

        notifyItemChanged(0);
    }

    public void updateMoth(String month) {
        this.month = month;
        notifyItemChanged(0);
    }

    public String getMonth() {
        return month;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ITEM_TYPE_HEADER) {
            return new ViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.header_sum, parent, false));
        } else {
            return new ViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_sold, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        int itemViewType = getItemViewType(position);
        if (ITEM_TYPE_HEADER == itemViewType) {
            if (holder.monthTextView != null) {
                holder.monthTextView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onDatePickerClickedListener.onDatePickerClicked(v);
                    }
                });
                holder.monthTextView.setText(month);
            }
            if (holder.sumTextView != null) {
                holder.sumTextView.setText(sum);
            }
        } else if (itemViewType == ITEM_TYPE_NORMAL) {
            Sold sold = mValues.get(position);
            if (holder.dateTextView != null) {
                holder.dateTextView.setText(Utils.formatDate(sold.getSold(), Utils.DATE_FORMAT_YMD, Utils.DATE_FORMAT_YMD_U));
            }
            if (holder.bankcardTextView != null) {
                holder.bankcardTextView.setText(sold.getBankCard());
            }
            if (holder.amountTextView != null) {
                holder.amountTextView.setText(Utils.formatMoney(sold.getAmount()));
            }
            if (holder.balanceTextView != null) {
                holder.balanceTextView.setText(Utils.formatMoney(MathUtils.subtract(MathUtils.subtract(sold.getAmount(), sold.getServiceCharge()), sold.getExtraCharge())));
            }

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO: 2018/12/11 detail
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? ITEM_TYPE_HEADER : ITEM_TYPE_NORMAL;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public View itemView;

        /**
         * header view
         */
        @Nullable
        @BindView(R.id.month_tv)
        TextView monthTextView;
        @Nullable
        @BindView(R.id.sum_tv)
        TextView sumTextView;

        @BindView(R.id.date)
        @Nullable
        TextView dateTextView;
        @BindView(R.id.bankcard)
        @Nullable
        TextView bankcardTextView;
        @BindView(R.id.amount)
        @Nullable
        TextView amountTextView;
        @BindView(R.id.balance)
        @Nullable
        TextView balanceTextView;

        public ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            itemView = view;
        }
    }
}
