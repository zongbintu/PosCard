package com.tu.poscard.ui;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;

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
import com.tu.poscard.data.model.SelectMode;
import com.tu.poscard.data.model.WrapSettleType;
import com.tu.poscard.util.MathUtils;
import com.tu.poscard.util.Utils;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;
import timber.log.Timber;

/**
 * 增加交易流水
 */
public class SoldAddActivity extends BaseActivity implements SettleTypeAdapter.OnSettleTypeListener {
    @BindView(R.id.date_et)
    EditText dateEditText;
    @BindView(R.id.bankcard_et)
    EditText bankcardEditText;
    @BindView(R.id.amount_et)
    EditText amountEditText;
    @BindView(R.id.service_charge_et)
    EditText serviceChargeEditText;
    @BindView(R.id.extra_charge_et)
    EditText extraChargeEditText;

    BottomSheetDialog dialog;
    SettleTypeAdapter adapter;
    List<WrapSettleType> mData = new ArrayList<>();
    WrapSettleType settleType;
    /**
     * 日期选择Dialog
     */
    BottomSheetDialog dateMonthDialog;

    @Override
    int layoutId() {
        return R.layout.activity_sold_add;
    }

    @Override
    void initView() {
        setTitle(R.string.sold_add, true);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        dateEditText.setText(simpleDateFormat.format(new Date()));

        amountEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                refreshServiceCharge();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("settle", settleType);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        settleType = (WrapSettleType) savedInstanceState.getSerializable("settle");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == Navigator.REQUEST_CODE_SOLD_ADD) {
                List<Bankcard> bankcards = (List<Bankcard>) data.getSerializableExtra(Navigator.EXTRA_DATA);
                if (null != bankcards && bankcards.size() > 0) {
                    bankcardEditText.setFocusable(false);
                    StringBuilder sb = new StringBuilder();
                    for (Bankcard ban : bankcards) {
                        sb.append(Utils.bankcard(ban));
                        sb.append("\n");
                    }
                    bankcardEditText.setText(sb.toString());
                    bankcardEditText.setTag(bankcards);
                }
            }
        }
    }

    private void showSettleDialog() {
        if (dialog == null) {
            dialog = new BottomSheetDialog(this);
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(true);
            dialog.setContentView(R.layout.list);
            RecyclerView recyclerView = dialog.findViewById(R.id.list);
            if (recyclerView != null) {
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
                adapter = new SettleTypeAdapter(mData, this);
                recyclerView.setAdapter(adapter);
            }
        }
        if (!dialog.isShowing()) {
            if (mData.isEmpty()) {
                loadSettleType();
            }
            dialog.show();
        }
    }

    void loadSettleType() {
        mData.clear();
        PosCardApplication posCardApplication = (PosCardApplication) getApplication();

        if (posCardApplication.getSettleTypeList().isEmpty()) {
            SQLiteDatabase db = posCardApplication.dbHelper.getReadableDatabase();
            com.tencent.wcdb.Cursor cursor = db.rawQueryWithFactory(SQLiteDirectCursor.FACTORY, "select id,name,d_service_charge,d_extra_charge,t_service_charge,t_extra_charge from settle_type order by update_Time desc;", null, "settle_type");
            try {
                while (cursor.moveToNext()) {
                    WrapSettleType data = new WrapSettleType();
                    data.setName(cursor.getString(cursor.getColumnIndex("name")) + " - " + "T + 1");
                    data.setServiceCharge(new BigDecimal(cursor.getDouble(cursor.getColumnIndex("t_service_charge"))));
                    data.setExtraCharge(new BigDecimal(cursor.getDouble(cursor.getColumnIndex("t_extra_charge"))));
                    mData.add(data);

                    data = new WrapSettleType();
                    data.setName(cursor.getString(cursor.getColumnIndex("name")) + " - " + "D + 0");
                    data.setServiceCharge(new BigDecimal(cursor.getDouble(cursor.getColumnIndex("d_service_charge"))));
                    data.setExtraCharge(new BigDecimal(cursor.getDouble(cursor.getColumnIndex("d_extra_charge"))));
                    mData.add(data);
                }
            } catch (Exception e) {
                Timber.e(e);
            } finally {
                db.close();
            }
            posCardApplication.setSettleTypeList(mData);
        } else {
            mData.addAll(posCardApplication.getSettleTypeList());
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @OnClick({R.id.save_btn, R.id.settle_type_select_iv, R.id.bankcard_select_iv, R.id.date_et})
    void onClick(View v) {
        switch (v.getId()) {
            case R.id.bankcard_select_iv:
                // 2018/12/21 选择银行卡
                Navigator.startBankcardListActivity(this, Navigator.REQUEST_CODE_SOLD_ADD, SelectMode.MULTI);
                break;
            case R.id.save_btn:

                String sold = dateEditText.getText().toString();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

                BigDecimal amount = MathUtils.format(amountEditText.getText().toString());
                if (new BigDecimal(0).compareTo(amount) > 0) {
                    showError(amountEditText, R.string.msg_error_amount);
                    return;
                }
                try {
                    Date date = simpleDateFormat.parse(sold);

                    PosCardApplication posCardApplication = (PosCardApplication) getApplication();
                    SQLiteDatabase db = posCardApplication.dbHelper.getWritableDatabase();

                    ContentValues cv = new ContentValues();

                    cv.put("sold", sold);

                    cv.put("amount", amountEditText.getText().toString());
                    cv.put("service_charge", serviceChargeEditText.getText().toString());
                    cv.put("extra_charge", extraChargeEditText.getText().toString());

                    Object bankcardId = bankcardEditText.getTag();
                    int count = -1;
                    if (bankcardId != null) {
                        List<Bankcard> bankcardIds = (List<Bankcard>) bankcardId;
                        count = bankcardIds.size();
                        Iterator<Bankcard> iterator = bankcardIds.iterator();
                        while (iterator.hasNext()) {
                            Bankcard bankcard = iterator.next();
                            cv.put("bankcard_id", String.valueOf(bankcard.getId()));
                            cv.put("bankcard", Utils.bankcard(bankcard));
                            long rowId = db.insert("sold", null, cv);
                            if (rowId != -1) {
                                count--;
                            }
                        }

                    } else {
                        cv.put("bankcard", bankcardEditText.getText().toString());
                        long rowId = db.insert("sold", null, cv);
                        if (rowId != -1) {
                            count = 0;
                        }
                    }

                    db.close();
                    if (count == 0) {
                        toast(R.string.msg_success_add);
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        toast(R.string.msg_error_add);
                    }
                } catch (ParseException e) {
                    Timber.e(e);
                    showError(dateEditText, R.string.error_date);
                    dateEditText.requestFocus();
                }
                break;
            case R.id.settle_type_select_iv:
                showSettleDialog();
                break;
            case R.id.date_et:
                if (null == dateMonthDialog) {
                    dateMonthDialog = new BottomSheetDialog(this);
                    dateMonthDialog.setContentView(R.layout.layout_date_pciker);
                    View cancelBtn = dateMonthDialog.findViewById(R.id.cancel);
                    if (cancelBtn != null) cancelBtn.setOnClickListener(datePickerOnClickListener);
                    View confirmBtn = dateMonthDialog.findViewById(R.id.confirm);
                    if (confirmBtn != null) confirmBtn.setOnClickListener(datePickerOnClickListener);
                }
                if (!dateMonthDialog.isShowing())
                    dateMonthDialog.show();
                break;
        }
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
                    if (datePicker != null) {
                        dateEditText.setText(Utils.formatDate(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth(), Utils.DATE_FORMAT_YMD));
                    }


                    dateMonthDialog.dismiss();
                    break;
            }
        }
    };

    @Override
    public void settleTypeChecked(WrapSettleType settleType) {
        this.settleType = settleType;
        dialog.dismiss();
        refreshServiceCharge();
    }

    void refreshServiceCharge() {
        BigDecimal amount = MathUtils.format(amountEditText.getText().toString());

        if (null != settleType) {
            if (amount.compareTo(new BigDecimal(0)) == 1) {
                serviceChargeEditText.setText(MathUtils.toString(amount.multiply(settleType.getServiceCharge().divide(new BigDecimal(100)))));
                extraChargeEditText.setText(MathUtils.toString(settleType.getExtraCharge()));
            }
        }
    }
}
