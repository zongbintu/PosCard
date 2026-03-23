package com.tu.poscard.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.tu.poscard.R;
import com.tu.poscard.data.model.CardTypeEnum;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * 银行卡类型RecyclerViewAdapter
 */
public class CardTypeAdapter extends RecyclerView.Adapter<CardTypeAdapter.ViewHolder> {


    OnCardTypeChangedListener listener;

    public CardTypeAdapter(OnCardTypeChangedListener listener) {
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tv, parent, false));

    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final CardTypeEnum cardTypeEnum = CardTypeEnum.values()[position];
        holder.textView.setText(cardTypeEnum.message());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onChanged(cardTypeEnum);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return CardTypeEnum.values().length;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public View itemView;
        @BindView(R.id.tv)
        TextView textView;

        public ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            itemView = view;
        }
    }

    interface OnCardTypeChangedListener {
        void onChanged(CardTypeEnum cardTypeEnum);
    }
}
