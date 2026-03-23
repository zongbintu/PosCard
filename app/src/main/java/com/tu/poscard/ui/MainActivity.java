package com.tu.poscard.ui;

import android.content.ContentValues;
import android.content.Intent;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.navigation.NavigationView;
import com.tencent.wcdb.database.SQLiteDatabase;
import com.tencent.wcdb.database.SQLiteDirectCursor;
import com.tu.poscard.Navigator;
import com.tu.poscard.PosCardApplication;
import com.tu.poscard.R;
import com.tu.poscard.data.model.Statement;
import com.tu.poscard.util.MathUtils;
import com.tu.poscard.util.Utils;
import com.tu.poscard.widget.DatePickerUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import timber.log.Timber;

public class MainActivity extends BaseActivity
    implements NavigationView.OnNavigationItemSelectedListener,
    StatementAdapter.OnStatementChangeListener, OnDatePickerClickedListener {
  @BindView(R.id.drawer_layout) DrawerLayout drawer;
  @BindView(R.id.nav_view) NavigationView navigationView;

  @BindView(R.id.list) RecyclerView recyclerView;

  StatementAdapter adapter;

  BottomSheetDialog dateMonthDialog;
  private long exitTime = 0;

  @Override int layoutId() {
    return R.layout.activity_main;
  }

  @Override void initView() {
    setTitle(R.string.app_name, false);

    ActionBarDrawerToggle toggle =
        new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open,
            R.string.navigation_drawer_close);
    drawer.addDrawerListener(toggle);
    toggle.syncState();

    navigationView.setNavigationItemSelectedListener(this);

    recyclerView.setLayoutManager(new LinearLayoutManager(this));
    adapter = new StatementAdapter(this, new ArrayList<Statement>(), this, this);

    recyclerView.setAdapter(adapter);

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(new Date());
    loadData(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH));
  }

  @Override public void onBackPressed() {
    if (drawer.isDrawerOpen(GravityCompat.START)) {
      drawer.closeDrawer(GravityCompat.START);
    } else {
      super.onBackPressed();
    }
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();

    if (id == R.id.action_statement_add) {
      Navigator.startStatementAddActivity(this, Navigator.REQUEST_CODE_STATEMENT_LIST);
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @SuppressWarnings("StatementWithEmptyBody") @Override
  public boolean onNavigationItemSelected(MenuItem item) {
    // Handle navigation view item clicks here.
    int id = item.getItemId();
    switch (id) {
      case R.id.nav_bankcard:
        Navigator.startBankcardListActivity(this);
        break;
      case R.id.nav_sold:
        startActivity(new Intent(this, SoldListActivity.class));
        break;
      case R.id.nav_settle_type:
        startActivity(new Intent(this, SettleTypeAddActivity.class));
        break;
    }

    drawer.closeDrawer(GravityCompat.START);
    return true;
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
      case Navigator.REQUEST_CODE_STATEMENT_LIST:
        if (RESULT_OK == resultCode) {
          loadData();
        }
        break;
    }
  }

  @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      exit();
      return false;
    }
    return super.onKeyDown(keyCode, event);
  }

  public void exit() {
    if ((System.currentTimeMillis() - exitTime) > 2000) {
      toast(R.string.msg_exit);
      exitTime = System.currentTimeMillis();
    } else {
      finish();
      System.exit(0);
    }
  }

  @Override void onTitleDoubleClick() {
    super.onTitleDoubleClick();
    if (adapter.getItemCount() > 5) {
      recyclerView.smoothScrollToPosition(0);
    }
  }

  @OnClick({ R.id.fab }) void onClick(View view) {
    switch (view.getId()) {
      case R.id.fab:
        Navigator.startSoldAddActivity(this);
        break;
    }
  }

  private void loadData() {
    PosCardApplication posCardApplication = (PosCardApplication) getApplicationContext();
    SQLiteDatabase db = posCardApplication.dbHelper.getReadableDatabase();

    String[] monthArgs = Utils.getMonthStrar(adapter.getMonth());
    String where;
    if (monthArgs != null && monthArgs.length == 1) {
      where = " where payment_due_date>=?";
    } else {
      monthArgs = null;
      where = "";
    }

    com.tencent.wcdb.Cursor cursor = db.rawQueryWithFactory(SQLiteDirectCursor.FACTORY,
        "select id,bankcard,new_balance,min_payment,payment_due_date,new_payment,status,event_id from statement "
            + where
            + " order by status asc, payment_due_date asc;", monthArgs, "statement");
    try {
      adapter.clear();
      List<Statement> statements = new ArrayList<>();
      while (cursor.moveToNext()) {
        Statement data = new Statement();
        data.setId(cursor.getInt(cursor.getColumnIndex("id")));
        data.setBankcard(cursor.getString(cursor.getColumnIndex("bankcard")));
        data.setNew_balance(new BigDecimal(cursor.getDouble(cursor.getColumnIndex("new_balance"))));

        data.setMin_payment(new BigDecimal(cursor.getDouble(cursor.getColumnIndex("min_payment"))));
        data.setPayment_due_date(cursor.getString(cursor.getColumnIndex("payment_due_date")));
        data.setStatus(cursor.getInt(cursor.getColumnIndex("status")));
        data.setNew_payment(new BigDecimal(cursor.getDouble(cursor.getColumnIndex("new_payment"))));
        data.setEvent_id(cursor.getString(cursor.getColumnIndex("event_id")));
        statements.add(data);
        //处理数据
      }
      cursor.close();
      adapter.addAll(statements);
    } catch (Exception e) {
      Timber.e(e);
    } finally {
      db.close();
    }
    adapter.notifyDataSetChanged();

    sum(monthArgs);
  }

  private void sum(String[] monthArgs) {
    int count = 0, newCount = 0;
    BigDecimal sum = BigDecimal.ZERO, newSum = BigDecimal.ZERO, newPayment = BigDecimal.ZERO;

    PosCardApplication posCardApplication = (PosCardApplication) getApplicationContext();
    SQLiteDatabase db = posCardApplication.dbHelper.getReadableDatabase();

    String where;
    if (monthArgs != null && monthArgs.length == 1) {
      where = " where payment_due_date>=? ";
    } else {
      monthArgs = null;
      where = "";
    }

    com.tencent.wcdb.Cursor cursor = db.rawQueryWithFactory(SQLiteDirectCursor.FACTORY,
        "select count(id) as sc,sum(new_balance) as sNewBalance from statement " + where + ";",
        monthArgs, "statement");
    try {
      if (cursor.moveToNext()) {
        count = cursor.getInt(cursor.getColumnIndex("sc"));
        sum = new BigDecimal(cursor.getDouble(cursor.getColumnIndex("sNewBalance")));
      }
      cursor.close();
    } catch (Exception e) {
      Timber.e(e);
    } // finally {
    //            db.close();
    //        }

    if (TextUtils.isEmpty(where)) {
      where = " where new_balance>new_payment";
    } else {
      where += " and new_balance>new_payment";
    }

    cursor = db.rawQueryWithFactory(SQLiteDirectCursor.FACTORY,
        "select count(id) as sc,sum(new_balance) as sNewBalance,sum(new_payment) as sPayment from statement "
            + where
            + ";", monthArgs, "statement");
    try {
      if (cursor.moveToNext()) {
        newCount = cursor.getInt(cursor.getColumnIndex("sc"));
        newSum = new BigDecimal(cursor.getDouble(cursor.getColumnIndex("sNewBalance")));
        newPayment = new BigDecimal(cursor.getDouble(cursor.getColumnIndex("sPayment")));
      }
      cursor.close();
    } catch (Exception e) {
      Timber.e(e);
    } finally {
      db.close();
    }

    adapter.updateSum(
        getString(R.string.statement_sum_arg, String.valueOf(count), Utils.formatMoney(sum),
            String.valueOf(newCount), Utils.formatMoney(MathUtils.subtract(newSum, newPayment))));
  }

  private void loadData(int year, int month) {
    adapter.updateMoth(
        getString(R.string.ymu_arg, String.valueOf(year), String.valueOf(month + 1)));
    loadData();
  }

  @Override public void onChanged(int position, Statement statement) {
    PosCardApplication posCardApplication = (PosCardApplication) getApplicationContext();
    SQLiteDatabase db = posCardApplication.dbHelper.getWritableDatabase();

    try {
      ContentValues contentValues = new ContentValues();
      contentValues.put("status", statement.getStatus());
      contentValues.put("new_payment", statement.getNew_payment().toString());
      contentValues.put("update_time", Utils.formatDate(new Date(), Utils.DATE_FORMAT_YMDHMS));

      String whereClause = "id=?";
      //修改添加参数
      String[] whereArgs = { String.valueOf(statement.getId()) };
      int count = db.update("statement", contentValues, whereClause, whereArgs);
      if (count == 1) {
        adapter.notifyStatus(position, statement.getStatus());
        adapter.notifyItemChanged(position);

        sum(Utils.getMonthStrar(adapter.getMonth()));
      }
    } catch (Exception e) {
      Timber.e(e);
      toast(R.string.msg_error_update);
    } finally {
      db.close();
    }
  }

  @Override public void onDatePickerClicked(View view) {
    if (null == dateMonthDialog) {
      dateMonthDialog = new BottomSheetDialog(this);
      dateMonthDialog.setContentView(R.layout.layout_date_pciker);
      DatePickerUtil.hideDay((DatePicker) dateMonthDialog.findViewById(R.id.date_picker));
      dateMonthDialog.findViewById(R.id.cancel).setOnClickListener(datePickerOnClickListener);
      dateMonthDialog.findViewById(R.id.confirm).setOnClickListener(datePickerOnClickListener);
    }
    if (!dateMonthDialog.isShowing()) {
      dateMonthDialog.show();
    }
  }

  @Override public void onItemClick(Statement data) {
    Navigator.startStatementAddActivity(this, Navigator.REQUEST_CODE_STATEMENT_LIST, data);
  }

  View.OnClickListener datePickerOnClickListener = new View.OnClickListener() {
    @Override public void onClick(View v) {
      switch (v.getId()) {
        case R.id.cancel:
          dateMonthDialog.dismiss();
          break;
        case R.id.confirm:
          DatePicker datePicker = dateMonthDialog.findViewById(R.id.date_picker);

          loadData(datePicker.getYear(), datePicker.getMonth());

          dateMonthDialog.dismiss();
          break;
      }
    }
  };
}
