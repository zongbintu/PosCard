package com.tu.poscard.ui;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.tencent.wcdb.database.SQLiteDatabase;
import com.tencent.wcdb.database.SQLiteDirectCursor;
import com.tu.poscard.Navigator;
import com.tu.poscard.PosCardApplication;
import com.tu.poscard.R;
import com.tu.poscard.data.model.Bankcard;
import com.tu.poscard.data.model.CardTypeEnum;
import com.tu.poscard.data.model.PaymentStatusEnum;
import com.tu.poscard.data.model.SelectMode;
import com.tu.poscard.data.model.Statement;
import com.tu.poscard.util.MathUtils;
import com.tu.poscard.util.Utils;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import butterknife.BindView;
import butterknife.OnClick;
import timber.log.Timber;

/**
 * 增加账单
 */
public class StatementAddActivity extends BaseActivity {
  @BindView(R.id.bankcard_et) EditText bankcardEditText;
  @BindView(R.id.new_balance_et) EditText newBalanceEditText;
  @BindView(R.id.min_payment_et) EditText minPaymentEditText;
  @BindView(R.id.payment_due_date_et) EditText paymentDueDateEditText;
  @BindView(R.id.payment_et) EditText paymentEditText;

  /**
   * 日期选择Dialog
   */
  BottomSheetDialog dateMonthDialog;

  Statement statement;

  static final int PERMISSION_REQUEST_CODE = 11;
  String[] permissions = new String[] {
      Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR
  };

  /**
   * 检查是否已被授权危险权限
   */
  public boolean checkDangerousPermissions(Activity ac, String[] permissions) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      return true;
    }
    for (String permission : permissions) {
      if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
          || ActivityCompat.shouldShowRequestPermissionRationale(ac, permission)) {
        return false;
      }
    }
    return true;
  }

  @Override int layoutId() {
    return R.layout.activity_statement_add;
  }

  @Override void initView() {
    setTitle(R.string.statement_add, true);

    Object obj = getIntent().getSerializableExtra(Navigator.EXTRA_DATA);
    if (obj != null && obj instanceof Statement) {
      statement = (Statement) obj;
      bankcardEditText.setText(statement.getBankcard());
      if (null != statement.getBankcard_Id() && 0 < statement.getBankcard_Id()) {
        bankcardEditText.setTag(statement.getBankcard_Id());
      }
      newBalanceEditText.setText(MathUtils.toString(statement.getNew_balance()));
      minPaymentEditText.setText(
          BigDecimal.ZERO.compareTo(statement.getMin_payment()) == -1 ? MathUtils.toString(
              statement.getMin_payment()) : "");
      paymentDueDateEditText.setText(statement.getPayment_due_date());
      paymentEditText.setText(
          BigDecimal.ZERO.compareTo(statement.getNew_payment()) == -1 ? MathUtils.toString(
              statement.getNew_payment()) : "");
    }

    boolean permissionGrant = checkDangerousPermissions(this, permissions);
    if (!permissionGrant) {
      ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
    }
  }

  @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putSerializable(Navigator.EXTRA_DATA, statement);
  }

  @Override protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    statement = (Statement) savedInstanceState.getSerializable(Navigator.EXTRA_DATA);
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode == RESULT_OK) {
      if (requestCode == Navigator.REQUEST_CODE_STATEMENT_ADD) {
        List<Bankcard> bankcards = (List<Bankcard>) data.getSerializableExtra(Navigator.EXTRA_DATA);
        if (null != bankcards && bankcards.size() > 0) {
          Bankcard bankcard = bankcards.get(0);
          bankcardEditText.setText(Utils.bankcard(bankcard));
          bankcardEditText.setTag(bankcard.getId());
          if (paymentDueDateEditText.getText().length() == 0) {
            String paymentDueDate = Utils.paymentDueDate(bankcard);
            paymentDueDateEditText.setText(null != paymentDueDate ? paymentDueDate : "");
          }
          if (CardTypeEnum.DEBIT == CardTypeEnum.code(bankcard.getCard_type())
              && BigDecimal.ZERO.compareTo(bankcard.getCredit_limit()) != 0) {
            newBalanceEditText.setText(MathUtils.toString(bankcard.getCredit_limit()));
          }
        }
      }
    }
  }

  @OnClick({ R.id.save_btn, R.id.bankcard_select_iv, R.id.payment_due_date_et }) void onClick(
      View v) {
    switch (v.getId()) {
      case R.id.bankcard_select_iv:
        // 选择银行卡
        Navigator.startBankcardListActivity(this, Navigator.REQUEST_CODE_STATEMENT_ADD,
            SelectMode.SINGLE);
        break;
      case R.id.save_btn:
        String paymentDueDate = paymentDueDateEditText.getText().toString();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd", Locale.CHINA);
        try {
          Date date = simpleDateFormat.parse(paymentDueDate);

          PosCardApplication posCardApplication = (PosCardApplication) getApplication();
          SQLiteDatabase db = posCardApplication.dbHelper.getWritableDatabase();

          ContentValues cv = new ContentValues();
          cv.put("bankcard", bankcardEditText.getText().toString());
          Object bankcardId = bankcardEditText.getTag();
          if (bankcardId != null) {
            cv.put("bankcard_id", bankcardId.toString());
          }

          cv.put("new_balance", newBalanceEditText.getText().toString());
          cv.put("min_payment", minPaymentEditText.getText().toString());
          if (paymentEditText.getText().length() > 0) {
            cv.put("new_payment", paymentEditText.getText().toString());
          }
          cv.put("payment_due_date", paymentDueDate);
          PaymentStatusEnum paymentStatusEnum = Utils.paymentStatus(paymentDueDate,
              MathUtils.format(newBalanceEditText.getText().toString()),
              MathUtils.format(paymentEditText.getText().toString()));
          cv.put("status", paymentStatusEnum.code());

          boolean isSuccess;
          if (statement != null) {
            long count = db.updateWithOnConflict("statement", cv, "id=?",
                new String[] { String.valueOf(statement.getId()) }, SQLiteDatabase.CONFLICT_NONE);
            isSuccess = count == 1;
          } else {
            long rowId = db.insert("statement", null, cv);
            isSuccess = rowId != -1;
            com.tencent.wcdb.Cursor cursor = db.rawQueryWithFactory(SQLiteDirectCursor.FACTORY,
                "select id,bankcard,new_balance,min_payment,payment_due_date,new_payment,status,event_id from statement "
                    + " where id = ? ;", new String[]{String.valueOf(rowId)}, "statement");
            try {
              while (cursor.moveToNext()) {
                statement = new Statement();
                statement.setId(cursor.getInt(cursor.getColumnIndex("id")));
                statement.setBankcard(cursor.getString(cursor.getColumnIndex("bankcard")));
                statement.setNew_balance(new BigDecimal(cursor.getDouble(cursor.getColumnIndex("new_balance"))));

                statement.setMin_payment(new BigDecimal(cursor.getDouble(cursor.getColumnIndex("min_payment"))));
                statement.setPayment_due_date(cursor.getString(cursor.getColumnIndex("payment_due_date")));
                statement.setStatus(cursor.getInt(cursor.getColumnIndex("status")));
                statement.setNew_payment(new BigDecimal(cursor.getDouble(cursor.getColumnIndex("new_payment"))));
                statement.setEvent_id(cursor.getString(cursor.getColumnIndex("event_id")));
              }
              cursor.close();
            } catch (Exception e) {
              Timber.e(e);
            }
          }

          if (isSuccess) {
            if(null != statement){
              String eventId = statement.getEvent_id();
              eventId = addEvent(eventId,bankcardEditText.getText().toString(), date,
                  newBalanceEditText.getText().toString());
              if (eventId != null && !eventId.equals(statement.getEvent_id())) {
                ContentValues eventCV = new ContentValues();
                eventCV.put("event_id", eventId);
                db.updateWithOnConflict("statement", eventCV, "id=?",
                    new String[] { String.valueOf(statement.getId()) }, SQLiteDatabase.CONFLICT_NONE);
              }
            }
            toast(R.string.msg_success_add);
            setResult(RESULT_OK);
            finish();
          } else {
            toast(R.string.msg_error_add);
          }
          db.close();
        } catch (ParseException e) {
          Timber.e(e);
          toast(R.string.error_date);
          paymentDueDateEditText.requestFocus(paymentDueDateEditText.length());
        }
        break;
      case R.id.payment_due_date_et:
        if (null == dateMonthDialog) {
          dateMonthDialog = new BottomSheetDialog(this);
          dateMonthDialog.setContentView(R.layout.layout_date_pciker);
          dateMonthDialog.findViewById(R.id.cancel).setOnClickListener(datePickerOnClickListener);
          dateMonthDialog.findViewById(R.id.confirm).setOnClickListener(datePickerOnClickListener);
        }
        if (!dateMonthDialog.isShowing()) {
          dateMonthDialog.show();
        }
        break;
    }
  }

  View.OnClickListener datePickerOnClickListener = new View.OnClickListener() {
    @Override public void onClick(View v) {
      switch (v.getId()) {
        case R.id.cancel:
          dateMonthDialog.dismiss();
          break;
        case R.id.confirm:
          DatePicker datePicker = dateMonthDialog.findViewById(R.id.date_picker);
          paymentDueDateEditText.setText(
              Utils.formatDate(datePicker.getYear(), datePicker.getMonth(),
                  datePicker.getDayOfMonth(), Utils.DATE_FORMAT_YMD));

          dateMonthDialog.dismiss();
          break;
      }
    }
  };

  String addEvent(String eventId,String bankcardName, Date paymentDueDate, String new_balance) {
    if (checkDangerousPermissions(this, permissions)) {
      if(!TextUtils.isEmpty(eventId)){
        ContentValues values = new ContentValues();
        values.put(CalendarContract.Events.TITLE, bankcardName + "-￥" + new_balance);
        Uri updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, Long.parseLong(eventId));
        int rows = getContentResolver().update(updateUri, values, null, null);
        if(rows >0) {
          return eventId;
        }
      }
      ContentResolver cr = getContentResolver();
      ContentValues values = new ContentValues();
      Calendar calendarStart = Calendar.getInstance();
      calendarStart.setTime(paymentDueDate);
      calendarStart.add(Calendar.HOUR, 8);
      values.put(CalendarContract.Events.DTSTART, calendarStart.getTimeInMillis());
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(paymentDueDate);
      calendar.add(Calendar.DAY_OF_MONTH, 1);
      values.put(CalendarContract.Events.DTEND, calendar.getTimeInMillis());
      values.put(CalendarContract.Events.TITLE, bankcardName + "-￥" + new_balance);
      values.put(CalendarContract.Events.CALENDAR_ID, 1);
      values.put(CalendarContract.Events.ALL_DAY, true);
      values.put(CalendarContract.Events.HAS_ALARM, true);

      values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
      Uri uri = cr.insert(CalendarContract.Events.CONTENT_URI, values);

      if (uri == null) return null;
      String eventId2 = uri.getLastPathSegment();
      ContentResolver reminderCR = getContentResolver();
      ContentValues reminderValues = new ContentValues();
      reminderValues.put(CalendarContract.Reminders.EVENT_ID, eventId2);
      reminderValues.put(CalendarContract.Reminders.MINUTES, -1);
      reminderValues.put(CalendarContract.Reminders.METHOD,
          CalendarContract.Reminders.METHOD_ALERT);
      reminderCR.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues);

      return eventId2;
    }
    return null;
  }
}
