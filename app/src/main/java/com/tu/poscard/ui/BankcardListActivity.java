package com.tu.poscard.ui;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tencent.wcdb.database.SQLiteDatabase;
import com.tencent.wcdb.database.SQLiteDirectCursor;
import com.tu.poscard.Navigator;
import com.tu.poscard.PosCardApplication;
import com.tu.poscard.R;
import com.tu.poscard.data.model.Bankcard;
import com.tu.poscard.data.model.SelectMode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import timber.log.Timber;

/**
 * 银行卡ListActivity
 */
public class BankcardListActivity extends BaseActivity implements BankcardAdapter.OnBankcardSelectedListener {
    @BindView(R.id.list)
    RecyclerView recyclerView;

    List<Bankcard> mData = new ArrayList<>();
    BankcardAdapter adapter;

    SelectMode selectMode;

    @Override
    void initView() {
        setTitle(R.string.bankcard, true);

        selectMode = (SelectMode) getIntent().getSerializableExtra(Navigator.EXTRA_SELECT);
        selectMode = selectMode == null ? SelectMode.NONE : selectMode;
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BankcardAdapter(mData, this, selectMode);
        recyclerView.setAdapter(adapter);

        loadData();
    }

    @Override
    int layoutId() {
        return R.layout.activity_list;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(SelectMode.NONE.equals(selectMode) ? R.menu.add : R.menu.confirm, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_add:
                Navigator.bankcardAddActivity(this, Navigator.REQUEST_CODE_BANKCARD_LIST);
                return true;
            case R.id.action_ok:
                //选择
                Intent data = new Intent();
                data.putExtra(Navigator.EXTRA_DATA, (Serializable) adapter.getSelectedBankcards());
                setResult(RESULT_OK, data);
                finish();

                break;
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
        if (resultCode == RESULT_OK) {
            loadData();
        }
    }


    private void loadData() {
        PosCardApplication posCardApplication = (PosCardApplication) getApplicationContext();
        SQLiteDatabase db = posCardApplication.dbHelper.getReadableDatabase();
        //账单顺延，会造成排序错误
        com.tencent.wcdb.Cursor cursor = db.rawQueryWithFactory(SQLiteDirectCursor.FACTORY,
                "select id,bank,name,nik_name,cardno,statement_date,payment_due_date,payment_due_day,card_type,credit_limit,description from bank_card order by  cast(payment_due_date as '9999');", null, "bank_card");
        try {
            mData.clear();
            while (cursor.moveToNext()) {
                Bankcard data = new Bankcard();
                data.setId(cursor.getInt(cursor.getColumnIndex("id")));
                data.setBank(cursor.getString(cursor.getColumnIndex("bank")));
                data.setName(cursor.getString(cursor.getColumnIndex("name")));

                data.setNik_name(cursor.getString(cursor.getColumnIndex("nik_name")));
                data.setCardno(cursor.getString(cursor.getColumnIndex("cardno")));
                data.setStatement_date(cursor.getString(cursor.getColumnIndex("statement_date")));
                data.setPayment_due_date(cursor.getString(cursor.getColumnIndex("payment_due_date")));
                data.setPayment_due_day(cursor.getString(cursor.getColumnIndex("payment_due_day")));
                data.setCard_type(cursor.getString(cursor.getColumnIndex("card_type")));
                data.setCredit_limit(new BigDecimal(cursor.getDouble(cursor.getColumnIndex("credit_limit"))));
                data.setDescription(cursor.getString(cursor.getColumnIndex("description")));

                mData.add(data);
            }
            cursor.close();
        } catch (Exception e) {
            Timber.e(e);
        } finally {
            db.close();
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onSelected(Bankcard bankcard) {
        if (SelectMode.NONE.equals(selectMode)) {
            Navigator.bankcardActivity(this, Navigator.REQUEST_CODE_BANKCARD_LIST, bankcard);
        } else {
            //选择
            Intent data = new Intent();
            List<Bankcard> bankcards = new ArrayList<>(1);
            bankcards.add(bankcard);
            data.putExtra(Navigator.EXTRA_DATA, (Serializable) bankcards);
            setResult(RESULT_OK, data);
            finish();
        }
    }
}
