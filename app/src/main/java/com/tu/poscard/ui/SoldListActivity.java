package com.tu.poscard.ui;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.tencent.wcdb.database.SQLiteDatabase;
import com.tencent.wcdb.database.SQLiteDirectCursor;
import com.tu.poscard.Navigator;
import com.tu.poscard.PosCardApplication;
import com.tu.poscard.R;
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

/**
 * 交易ListActivity
 */
public class SoldListActivity extends BaseActivity implements OnDatePickerClickedListener {
    @BindView(R.id.list)
    RecyclerView recyclerView;
    private List<Sold> mData = new ArrayList<>();
    SoldAdapter adapter;

    BottomSheetDialog dateMonthDialog;

    @Override
    int layoutId() {
        return R.layout.activity_list;
    }

    @Override
    void initView() {
        setTitle(R.string.sold_list, true);

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
        getMenuInflater().inflate(R.menu.add, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_add:
                Navigator.startSoldAddActivity(this, Navigator.REQUEST_CODE_SOLD_LIST);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Navigator.REQUEST_CODE_SOLD_LIST) {
            if (RESULT_OK == resultCode) {
                loadSold();
            }
        }
    }

    private void sum(String[] monthArgs) {
        PosCardApplication posCardApplication = (PosCardApplication) getApplicationContext();
        SQLiteDatabase db = posCardApplication.dbHelper.getReadableDatabase();

        String where;
        if (monthArgs != null && monthArgs.length == 2) {
            where = " where sold>=? and sold<=?";
        } else {
            monthArgs = null;
            where = "";
        }

        com.tencent.wcdb.Cursor cursor = db.rawQueryWithFactory(SQLiteDirectCursor.FACTORY, "select sum(amount) as samount,sum(service_Charge) as sServiceCharge,sum(extra_Charge) as sExtraCharge from sold" + where + " order by sold desc,update_Time desc;", monthArgs, "sold");

        try {
            if (cursor.moveToNext()) {
                BigDecimal amount = new BigDecimal(cursor.getDouble(cursor.getColumnIndex("samount")));
                BigDecimal sServiceCharge = new BigDecimal(cursor.getDouble(cursor.getColumnIndex("sServiceCharge")));
                BigDecimal sExtraCharge = new BigDecimal(cursor.getDouble(cursor.getColumnIndex("sExtraCharge")));
                adapter.updateSum(getString(R.string.sold_sum_arg, Utils.formatMoney(amount), Utils.formatMoney(sServiceCharge), Utils.formatMoney(sExtraCharge), Utils.formatMoney(amount.subtract(sServiceCharge).subtract(sExtraCharge))));
            }
            cursor.close();
        } catch (Exception e) {
            Timber.e(e);
        } finally {
            db.close();
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

        sum(monthArgs);

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
            View cancelBtn = dateMonthDialog.findViewById(R.id.cancel);
            if (cancelBtn != null) cancelBtn.setOnClickListener(datePickerOnClickListener);
            View confirmBtn = dateMonthDialog.findViewById(R.id.confirm);
            if (confirmBtn != null) confirmBtn.setOnClickListener(datePickerOnClickListener);
        }
        if (!dateMonthDialog.isShowing())
            dateMonthDialog.show();
    }
}
