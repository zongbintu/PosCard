package com.tu.poscard.ui;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.tencent.wcdb.database.SQLiteDatabase;
import com.tu.poscard.Navigator;
import com.tu.poscard.PosCardApplication;
import com.tu.poscard.R;
import com.tu.poscard.data.model.Bankcard;
import com.tu.poscard.data.model.CardTypeEnum;
import com.tu.poscard.util.MathUtils;

import java.math.BigDecimal;

import butterknife.BindView;
import butterknife.OnClick;
import io.card.payment.CardIOActivity;
import io.card.payment.CreditCard;

/**
 * 增加银行卡
 */
public class BankcardAddActivity extends BaseActivity implements CardTypeAdapter.OnCardTypeChangedListener {

    @BindView(R.id.name_et)
    EditText nameEditText;
    @BindView(R.id.bank_et)
    EditText bankEditText;
    @BindView(R.id.cardno_et)
    EditText cardNoEditText;
    @BindView(R.id.statement_date_et)
    EditText statementDateEditText;
    @BindView(R.id.payment_due_date_et)
    EditText paymentDueDateEditText;
    @BindView(R.id.payment_due_day_et)
    EditText paymentDueDayEditText;
    Bankcard bankcard;
    @BindView(R.id.card_type_et)
    EditText cardTypeEditText;
    @BindView(R.id.credit_limit_et)
    EditText creditLimitEditText;
    @BindView(R.id.description_et)
            EditText descriptionEditText;

    /**
     * 银行卡类型Dialog
     */
    BottomSheetDialog cardTypeDialog;
    CardTypeAdapter adapter;

    @Override
    int layoutId() {
        return R.layout.activity_bankcard_add;
    }

    @Override
    void initView() {
        setTitle(R.string.bankcard_edit, true);

        Object obj = getIntent().getSerializableExtra(Navigator.EXTRA_DATA);
        if (obj != null && obj instanceof Bankcard) {
            bankcard = (Bankcard) obj;
            nameEditText.setText(bankcard.getName());
            bankEditText.setText(bankcard.getBank());
            cardNoEditText.setText(bankcard.getCardno());
            statementDateEditText.setText(bankcard.getStatement_date());
            paymentDueDateEditText.setText(bankcard.getPayment_due_date());
            paymentDueDayEditText.setText(null != bankcard.getPayment_due_day() ? bankcard.getPayment_due_day() : "");

            if (null != CardTypeEnum.code(bankcard.getCard_type())) {
                onChanged(CardTypeEnum.code(bankcard.getCard_type()));
            }

            if(BigDecimal.ZERO.compareTo(bankcard.getCredit_limit()) != 0) {
                creditLimitEditText.setText(MathUtils.toString(bankcard.getCredit_limit()));
            }
            descriptionEditText.setText(bankcard.getDescription());
        } else {
            onChanged(CardTypeEnum.CREDIT);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(Navigator.EXTRA_DATA, bankcard);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        bankcard = (Bankcard) savedInstanceState.getSerializable(Navigator.EXTRA_DATA);
    }

    @OnClick({R.id.save_btn, R.id.bankcard_scan_iv, R.id.card_type_et})
    void onClick(View v) {
        switch (v.getId()) {
            case R.id.save_btn:
            if (nameEditText.getText().length() < 1) {
                showError(nameEditText, R.string.hint_name);
                return;
            }
            if (bankEditText.getText().length() < 1) {
                showError(bankEditText, R.string.hint_bank);
                return;
            }
            PosCardApplication posCardApplication = (PosCardApplication) getApplication();
            SQLiteDatabase db = posCardApplication.dbHelper.getWritableDatabase();

            ContentValues cv = new ContentValues();

            cv.put("name", nameEditText.getText().toString());

            cv.put("bank", bankEditText.getText().toString());
            cv.put("cardno", cardNoEditText.getText().toString());
            cv.put("statement_date", statementDateEditText.getText().toString());
            cv.put("payment_due_date", paymentDueDateEditText.getText().toString());
            Object cardTypeObj = cardTypeEditText.getTag();
            if(cardTypeObj !=null && cardTypeObj instanceof CardTypeEnum) {
                cv.put("card_type", ((CardTypeEnum)cardTypeObj).code());
            }
            cv.put("credit_limit",creditLimitEditText.getText().toString());
            cv.put("description",descriptionEditText.getText().toString());


            if (0 < paymentDueDayEditText.getText().length()) {
                cv.put("payment_due_day", paymentDueDayEditText.getText().toString());
            }

            boolean isSuccess;
            if (bankcard != null) {
                long count = db.updateWithOnConflict("bank_card", cv, "id=?", new String[]{String.valueOf(bankcard.getId())}, SQLiteDatabase.CONFLICT_NONE);
                isSuccess = count == 1;
            } else {
                long rowId = db.insert("bank_card", null, cv);
                isSuccess = rowId != -1;
            }
            db.close();
            if (isSuccess) {
                toast(R.string.msg_success_add);
                setResult(RESULT_OK);
                finish();
            } else {
                toast(R.string.msg_error_add);
            }
            break;
            case R.id.card_type_et:
                showCardTypeDialog();
                break;
            case R.id.bankcard_scan_iv:
                Navigator.startCardIOActivity(this);
                break;
        }
    }

    private void showCardTypeDialog() {
        if (cardTypeDialog == null) {
            cardTypeDialog = new BottomSheetDialog(this);
            cardTypeDialog.setCancelable(false);
            cardTypeDialog.setCanceledOnTouchOutside(true);
            cardTypeDialog.setContentView(R.layout.list);
            RecyclerView recyclerView = cardTypeDialog.findViewById(R.id.list);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
            adapter = new CardTypeAdapter(this);
            recyclerView.setAdapter(adapter);
        }
        if (!cardTypeDialog.isShowing()) {
            cardTypeDialog.show();
        }
    }

    @Override
    public void onChanged(CardTypeEnum cardTypeEnum) {
        cardTypeEditText.setTag(cardTypeEnum);
        cardTypeEditText.setText(cardTypeEnum.message());

        switch (cardTypeEnum){
            case DEBIT:
                statementDateEditText.setVisibility(View.GONE);
                paymentDueDayEditText.setVisibility(View.GONE);
                break;

            case CREDIT:
                statementDateEditText.setVisibility(View.VISIBLE);
                paymentDueDayEditText.setVisibility(View.VISIBLE);
                break;
        }
        if(cardTypeDialog != null) {
            cardTypeDialog.dismiss();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Navigator.REQUEST_CODE_BANKCARD_SCAN && data != null
                && data.hasExtra(CardIOActivity.EXTRA_SCAN_RESULT)) {
            CreditCard result = data.getParcelableExtra(CardIOActivity.EXTRA_SCAN_RESULT);
            if (result != null) {
                cardNoEditText.setText(result.cardNumber);
            }
        }
    }
}
