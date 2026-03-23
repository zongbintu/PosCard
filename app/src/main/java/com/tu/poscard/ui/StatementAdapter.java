package com.tu.poscard.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.tu.poscard.R;
import com.tu.poscard.data.model.PaymentStatusEnum;
import com.tu.poscard.data.model.Statement;
import com.tu.poscard.util.MathUtils;
import com.tu.poscard.util.Utils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * 银行卡RecyclerViewAdapter
 */
public class StatementAdapter extends RecyclerView.Adapter<StatementAdapter.ViewHolder> {

    private static final int ITEM_TYPE_HEADER = 1;
    private static final int ITEM_TYPE_NORMAL = 2;

    OnStatementChangeListener onStatementChangeListener;
    OnDatePickerClickedListener onDatePickerClickedListener;
    Context context;

    private final List<Statement> mValues;
    /**
     * 还款日期
     */
    String month = "";
    String sum = "";

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

    public StatementAdapter(Context context, List<Statement> items, OnStatementChangeListener onStatementChangeListener,OnDatePickerClickedListener onDatePickerClickedListener) {
        this.context = context;
        mValues = items;
        this.onStatementChangeListener = onStatementChangeListener;
        this.onDatePickerClickedListener = onDatePickerClickedListener;
    }

    public void clear() {
        mValues.clear();
    }

    public void addAll(List<Statement> statements) {
        mValues.clear();
        mValues.add(new Statement());
        mValues.addAll(statements);
    }

    public void notifyStatus(int position, int status) {
        mValues.get(position).setStatus(status);
        mValues.get(position).setNew_payment(mValues.get(position).getNew_balance());
        notifyItemChanged(position);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ITEM_TYPE_HEADER) {
            return new ViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.header_sum, parent, false));
        } else {
            return new ViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_statement, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
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
            final Statement data = mValues.get(position);
            if (holder.bankcardTextView != null) {
                holder.bankcardTextView.setText(data.getBankcard());
            }
            if (holder.paymentDueDateTextView != null) {
                holder.paymentDueDateTextView.setText(Utils.formatDate(data.getPayment_due_date(), Utils.DATE_FORMAT_YMD, Utils.DATE_FORMAT_MD_U));
            }
            if (holder.newBalanceTextView != null) {
                holder.newBalanceTextView.setText(Utils.formatMoney(data.getNew_balance()));
            }
            if (holder.haveBalanceTextView != null) {
                holder.haveBalanceTextView.setText(Utils.formatMoney(MathUtils.subtract(data.getNew_balance(),data.getNew_payment())));
            }

            Integer day = Utils.paymentDay(data.getPayment_due_date());
            final PaymentStatusEnum paymentStatusEnum = Utils.paymentStatus(data.getPayment_due_date(), data.getNew_balance(), data.getNew_payment());

            if (holder.statusTextView != null) {
                holder.statusTextView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (PaymentStatusEnum.REPAYMENT_PENDING.equals(paymentStatusEnum) || PaymentStatusEnum.OVERDUE.equals(paymentStatusEnum)) {
                            Statement statement = new Statement();
                            statement.setId(data.getId());
                            statement.setStatus(PaymentStatusEnum.PAY_OFF.code());
                            statement.setNew_payment(data.getNew_balance());
                            onStatementChangeListener.onChanged(position, statement);
                        }
                    }
                });

                holder.statusTextView.setText(paymentStatusEnum.message());
            }

            if (PaymentStatusEnum.PAY_OFF.equals(paymentStatusEnum)) {
                if (holder.statementDayContainer != null) {
                    holder.statementDayContainer.setVisibility(View.INVISIBLE);
                }
                if (holder.statusTextView != null) {
                    holder.statusTextView.setBackgroundResource(android.R.color.transparent);
                }
            } else if (PaymentStatusEnum.REPAYMENT_PENDING.equals(paymentStatusEnum)) {

                if (holder.statusTextView != null) {
                    holder.statusTextView.setBackgroundResource(R.drawable.bg_radius_button);
                }


                if (holder.newStatementDayTextView != null) {
                    if (null == day || day == 0) {
                        holder.newStatementDayTextView.setText(R.string.today);
                        holder.newStatementDayTextView.setTextColor(ContextCompat.getColor(context, R.color.orange));
                    } else if (day > 0) {
                        holder.newStatementDayTextView.setText(String.valueOf(day));
                        holder.newStatementDayTextView.setTextColor(ContextCompat.getColor(context, day > 5 ? R.color.black : R.color.orange));
                    }
                }
                if (holder.dayLabelTextView != null) {
                    holder.dayLabelTextView.setText(R.string.label_statement_day);
                }
                if (holder.statementDayContainer != null) {
                    holder.statementDayContainer.setVisibility(View.VISIBLE);
                }
            } else {
                if (holder.statementDayContainer != null) {
                    holder.statementDayContainer.setVisibility(View.VISIBLE);
                }
                if (holder.statusTextView != null) {
                    holder.statusTextView.setBackgroundResource(R.drawable.bg_radius_button_red);
                }

                //已逾期
                if (holder.newStatementDayTextView != null) {
                    holder.newStatementDayTextView.setText(String.valueOf(Math.abs(day)));
                    holder.newStatementDayTextView.setTextColor(ContextCompat.getColor(context, R.color.red));
                }
                if (holder.dayLabelTextView != null) {
                    holder.dayLabelTextView.setText(R.string.day_overdue);
                }
            }


            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onStatementChangeListener.onItemClick(data);
                }
            });
        }
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? ITEM_TYPE_HEADER : ITEM_TYPE_NORMAL;
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public View itemView;
        @Nullable
        @BindView(R.id.bankcard)
        TextView bankcardTextView;
        @Nullable
        @BindView(R.id.payment_due_date_tv)
        TextView paymentDueDateTextView;
        @Nullable
        @BindView(R.id.new_statement_day_tv)
        TextView newStatementDayTextView;
        @Nullable
        @BindView(R.id.new_balance_tv)
        TextView newBalanceTextView;
        @Nullable
        @BindView(R.id.have_balance_tv)
        TextView haveBalanceTextView;
        @Nullable
        @BindView(R.id.status_tv)
        TextView statusTextView;
        @Nullable
        @BindView(R.id.day_label_tv)
        TextView dayLabelTextView;
        @Nullable
        @BindView(R.id.statement_day_container)
        View statementDayContainer;

        /**
         * header view
         */
        @Nullable
        @BindView(R.id.month_tv)
        TextView monthTextView;
        @Nullable
        @BindView(R.id.sum_tv)
        TextView sumTextView;

        public ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            itemView = view;
        }
    }

    interface OnStatementChangeListener {
        void onChanged(int position, Statement statement);

        void onItemClick(Statement data);
    }
}
