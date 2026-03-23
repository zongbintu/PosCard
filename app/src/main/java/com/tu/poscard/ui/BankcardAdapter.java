package com.tu.poscard.ui;

import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.tu.poscard.R;
import com.tu.poscard.data.model.Bankcard;
import com.tu.poscard.data.model.CardTypeEnum;
import com.tu.poscard.data.model.SelectMode;
import com.tu.poscard.util.Utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * 银行卡RecyclerViewAdapter
 */
public class BankcardAdapter extends RecyclerView.Adapter<BankcardAdapter.ViewHolder> {

    private final List<Bankcard> mValues;
    OnBankcardSelectedListener onBankcardSelectedListener;
    /**
     * 0 单选 1多选
     */
    SelectMode selectMode;
    Set<Integer> selectedPositions = new HashSet<>();

    public BankcardAdapter(List<Bankcard> items, OnBankcardSelectedListener listener, SelectMode selectMode) {
        mValues = items;
        this.onBankcardSelectedListener = listener;
        this.selectMode = selectMode;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bankcard, parent, false));

    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final Bankcard data = mValues.get(position);

        if (selectedPositions.contains(position)) {

            holder.itemView.setCardBackgroundColor(holder.itemView.getResources().getColor(R.color.lighter_gray));
        } else {
            if (null != holder.colorStateList) {
                holder.itemView.setCardBackgroundColor(holder.colorStateList);
            } else {
                holder.itemView.setCardBackgroundColor(holder.itemView.getResources().getColor(android.R.color.transparent));
            }

        }

        final String bank = data.getBank() + (null == CardTypeEnum.code(data.getCard_type()) ? "" : " - " + CardTypeEnum.code(data.getCard_type()).message());
        holder.bankcardTextView.setText(bank);
        holder.nameTextView.setText(data.getName());
        holder.cardnoTextView.setText(Utils.formatCardNo(data.getCardno()));
        if (BigDecimal.ZERO.compareTo(data.getCredit_limit()) != 0) {
            holder.creditLimitTextView.setText(Utils.formatMoney(data.getCredit_limit()));
            holder.creditLimitContainer.setVisibility(View.VISIBLE);
        } else {
            holder.creditLimitContainer.setVisibility(View.GONE);
        }
        holder.statementDateTextView.setText(TextUtils.isEmpty(data.getStatement_date()) ? "无" : data.getStatement_date());
        if (!TextUtils.isEmpty(data.getPayment_due_date())) {
            holder.paymentDueDateTextView.setText(data.getPayment_due_date());
            holder.labelDueDayTextView.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(data.getPayment_due_day())) {
            holder.paymentDueDateTextView.setText(data.getPayment_due_day());
            holder.labelDueDayTextView.setVisibility(View.VISIBLE);
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onBankcardSelectedListener != null) {
                    if (SelectMode.MULTI.equals(selectMode)) {
                        boolean isSelected = selectedPositions.contains(position);
                        if (isSelected) {
                            selectedPositions.remove(position);
                        } else {
                            selectedPositions.add(position);
                        }
                        notifyItemChanged(position);
                    } else {
                        onBankcardSelectedListener.onSelected(data);
                    }
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public CardView itemView;
        ColorStateList colorStateList;
        @BindView(R.id.bank_tv)
        @Nullable
        TextView bankcardTextView;
        @BindView(R.id.name)
        @Nullable
        TextView nameTextView;
        @BindView(R.id.cardno_tv)
        @Nullable
        TextView cardnoTextView;
        @BindView(R.id.statement_date)
        @Nullable
        TextView statementDateTextView;
        @BindView(R.id.payment_due_date)
        @Nullable
        TextView paymentDueDateTextView;
        @BindView(R.id.payment_due_day_tv)
        TextView labelDueDayTextView;
        @BindView(R.id.credit_limit_tv)
        TextView creditLimitTextView;
        @BindView(R.id.credit_limit_container)
        View creditLimitContainer;

        public ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            itemView = (CardView) view;
            colorStateList = itemView.getCardBackgroundColor();
        }
    }

    public List<Bankcard> getSelectedBankcards() {
        Iterator<Integer> iterator = selectedPositions.iterator();
        List<Bankcard> bankcards = new ArrayList<>(selectedPositions.size());
        while (iterator.hasNext()) {
            Integer position = iterator.next();
            bankcards.add(mValues.get(position));
        }
        return bankcards;
    }

    interface OnBankcardSelectedListener {
        void onSelected(Bankcard bankcard);
    }
}
