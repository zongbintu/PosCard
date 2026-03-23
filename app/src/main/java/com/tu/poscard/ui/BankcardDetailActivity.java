package com.tu.poscard.ui;

import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.tencent.wcdb.database.SQLiteDatabase;
import com.tencent.wcdb.database.SQLiteDirectCursor;
import com.tu.poscard.Navigator;
import com.tu.poscard.PosCardApplication;
import com.tu.poscard.R;
import com.tu.poscard.data.model.Bankcard;
import com.tu.poscard.data.model.CardTypeEnum;
import com.tu.poscard.data.model.Sold;
import com.tu.poscard.util.Utils;
import com.tu.poscard.widget.DatePickerUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import timber.log.Timber;

public class BankcardDetailActivity extends BaseActivity implements OnDatePickerClickedListener {
    @BindView(R.id.bank_tv)
    TextView bankcardTextView;
    @BindView(R.id.name)
    TextView nameTextView;
    @BindView(R.id.cardno_tv)
    TextView cardnoTextView;
    @BindView(R.id.statement_date)
    TextView statementDateTextView;
    @BindView(R.id.payment_due_date)
    TextView paymentDueDateTextView;
    @BindView(R.id.payment_due_day_tv)
    TextView labelDueDayTextView;
    @BindView(R.id.credit_limit_tv)
    TextView creditLimitTextView;


    @BindView(android.R.id.list)
    RecyclerView recyclerView;
    List<Sold> mData = new ArrayList<>();
    SoldAdapter adapter;
    BottomSheetDialog dateMonthDialog;

    @Override
    int layoutId() {
        return R.layout.activity_bankcard_detail;
    }

    @Override
    void initView() {
        setTitle(R.string.bankcard, true);

        Bankcard data = (Bankcard) getIntent().getSerializableExtra(Navigator.EXTRA_DATA);
        final String bank = data.getBank() + (null == CardTypeEnum.code(data.getCard_type()) ? "" : " - " + CardTypeEnum.code(data.getCard_type()).message());
        bankcardTextView.setText(bank);
        nameTextView.setText(data.getName());
        cardnoTextView.setText(Utils.formatCardNo(data.getCardno()));
        if (BigDecimal.ZERO.compareTo(data.getCredit_limit()) != 0) {
            creditLimitTextView.setText(Utils.formatMoney(data.getCredit_limit()));
        } else {
            creditLimitTextView.setText("");
        }
        statementDateTextView.setText(TextUtils.isEmpty(data.getStatement_date()) ? "无" : data.getStatement_date());
        if (!TextUtils.isEmpty(data.getPayment_due_date())) {
            paymentDueDateTextView.setText(data.getPayment_due_date());
            labelDueDayTextView.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(data.getPayment_due_day())) {
            paymentDueDateTextView.setText(data.getPayment_due_day());
            labelDueDayTextView.setVisibility(View.VISIBLE);
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        mData.add(new Sold());
        adapter = new SoldAdapter(this, mData);
        recyclerView.setAdapter(adapter);

        Calendar now =
                Calendar.getInstance();
        now.setTime(new Date());
        onDateChange(now.get(Calendar.YEAR), now.get(Calendar.MONTH));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_edit:
                Navigator.bankcardAddActivity(this, Navigator.REQUEST_CODE_BANKCARD_DETAIL, (Bankcard) getIntent().getSerializableExtra(Navigator.EXTRA_DATA));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    void onTitleDoubleClick() {
        super.onTitleDoubleClick();
        if (adapter.getItemCount() > 5) {
            recyclerView.smoothScrollToPosition(0);
        }
    }

    private void loadSold() {
        PosCardApplication posCardApplication = (PosCardApplication) getApplicationContext();
        SQLiteDatabase db = posCardApplication.dbHelper.getReadableDatabase();

        String[] monthArgs = Utils.getMonth(adapter.getMonth());
        String where;
        if (monthArgs != null && monthArgs.length == 2) {
            where = " where sold>=? and sold<=?";
        } else {
            monthArgs = null;
            where = "";
        }
        com.tencent.wcdb.Cursor cursor = db.rawQueryWithFactory(SQLiteDirectCursor.FACTORY, "select id,sold,bankCard,amount,service_Charge,extra_Charge from sold" + where + " order by sold desc,update_Time desc;", monthArgs, "sold");

        try {
            mData.clear();
            mData.add(new Sold());
            while (cursor.moveToNext()) {
                Sold sold = new Sold();
                sold.setId(cursor.getInt(cursor.getColumnIndex("id")));
                sold.setSold(cursor.getString(cursor.getColumnIndex("sold")));
                sold.setBankCard(cursor.getString(cursor.getColumnIndex("bankCard")));
                sold.setAmount(new BigDecimal(cursor.getDouble(cursor.getColumnIndex("amount"))));
                sold.setServiceCharge(new BigDecimal(cursor.getDouble(cursor.getColumnIndex("service_Charge"))));
                sold.setExtraCharge(new BigDecimal(cursor.getDouble(cursor.getColumnIndex("extra_Charge"))));

                mData.add(sold);
                //处理数据
            }
            cursor.close();
        } catch (Exception e) {
            Timber.e(e);
        } finally {
            db.close();
        }
        adapter.notifyDataSetChanged();
    }

    private void onDateChange(int year, int month) {
        adapter.updateMoth(getString(R.string.ymu_arg, String.valueOf(year), String.valueOf(month + 1)));
        loadSold();
    }

    @Override
    public void onDatePickerClicked(View view) {
        if (null == dateMonthDialog) {
            dateMonthDialog = new BottomSheetDialog(this);
            dateMonthDialog.setContentView(R.layout.layout_date_pciker);
            DatePickerUtil.hideDay((DatePicker) dateMonthDialog.findViewById(R.id.date_picker));
            dateMonthDialog.findViewById(R.id.cancel).setOnClickListener(datePickerOnClickListener);
            dateMonthDialog.findViewById(R.id.confirm).setOnClickListener(datePickerOnClickListener);
        }
        if (!dateMonthDialog.isShowing())
            dateMonthDialog.show();
    }

    View.OnClickListener datePickerOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.cancel:
                    dateMonthDialog.dismiss();
                    break;
                case R.id.confirm:
                    DatePicker datePicker = dateMonthDialog.findViewById(R.id.date_picker);

                    onDateChange(datePicker.getYear(), datePicker.getMonth());

                    dateMonthDialog.dismiss();
                    break;
            }
        }
    };
}
