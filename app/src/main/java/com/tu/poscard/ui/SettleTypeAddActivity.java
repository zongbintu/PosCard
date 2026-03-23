package com.tu.poscard.ui;

import android.content.ContentValues;
import android.view.View;

import com.google.android.material.textfield.TextInputEditText;
import com.tencent.wcdb.database.SQLiteDatabase;
import com.tu.poscard.PosCardApplication;
import com.tu.poscard.R;
import com.tu.poscard.util.MathUtils;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * 结算方式新增Activity
 */
public class SettleTypeAddActivity extends BaseActivity {
    @BindView(R.id.name_et)
    TextInputEditText nameEditText;

    @BindView(R.id.t_service_charge_et)
    TextInputEditText t1ServiceChargeEditText;
    @BindView(R.id.t_extra_charge_et)
    TextInputEditText t1ExtraChargeEditText;
    @BindView(R.id.d_service_charge_et)
    TextInputEditText d0ServiceChargeEditText;
    @BindView(R.id.d_extra_charge_et)
    TextInputEditText d0ExtraChargeEditText;

    @Override
    int layoutId() {
        return R.layout.activity_settle_type_add;
    }

    @Override
    void initView() {
        setTitle(R.string.settle_type_add, true);
    }

    @OnClick(R.id.save_btn)
    void onClick(View v) {
        if (nameEditText.getText() == null || nameEditText.getText().length() < 1) {
            showError(nameEditText, R.string.hint_input_name);
            return;
        }
        PosCardApplication posCardApplication = (PosCardApplication) getApplication();
        SQLiteDatabase db = posCardApplication.dbHelper.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put("name", nameEditText.getText().toString());
        cv.put("t_service_charge", MathUtils.format(t1ServiceChargeEditText.getText().toString()).doubleValue());
        cv.put("t_extra_charge", MathUtils.format(t1ExtraChargeEditText.getText().toString()).doubleValue());
        cv.put("d_service_charge", MathUtils.format(d0ServiceChargeEditText.getText().toString()).doubleValue());
        cv.put("d_extra_charge", MathUtils.format(d0ExtraChargeEditText.getText().toString()).doubleValue());

        long rowId = db.insert("settle_type", null, cv);
        db.close();
        if (rowId != -1) {
            toast(R.string.msg_success_add);
            finish();
        } else {
            toast(R.string.msg_error_add);
        }
    }
}
