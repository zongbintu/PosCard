package com.tu.poscard.ui;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputLayout;
import com.tu.poscard.R;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @auther Tu
 * @date 2018/12/14
 * @email enum@foxmail.com
 */
public abstract class BaseActivity extends AppCompatActivity {
    Toast mToast;
    @BindView(R.id.toolbar)
    @Nullable
    Toolbar toolbar;
    private long lastToolbarClick;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layoutId());
        ButterKnife.bind(this);

        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (System.currentTimeMillis() - lastToolbarClick < 500) {
                        onTitleDoubleClick();
                    }
                    lastToolbarClick = System.currentTimeMillis();
                }
            });
        }
        initView();
    }

    abstract int layoutId();

    abstract void initView();

    /**
     * title 双击
     */
    void onTitleDoubleClick() {
    }


    void setTitle(int titleStrId, boolean displayHomeAsUpdateEnable) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeButtonEnabled(displayHomeAsUpdateEnable);
            getSupportActionBar().setDisplayHomeAsUpEnabled(displayHomeAsUpdateEnable);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        TextView textView = findViewById(R.id.toolbar_title);
        if (textView != null) {
            textView.setText(titleStrId);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void toast(CharSequence message) {
        synchronized (this) {
            if (mToast == null) {
                mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
            }
            mToast.setText(message);
            mToast.show();
        }
    }

    void toast(int strId) {
        toast(getString(strId));
    }

    void showError(EditText editText, int strId) {
        showError(editText, getString(strId));
    }

    void showError(EditText editText, String msg) {
        View parent = (View) editText.getParent().getParent();
        if (parent instanceof TextInputLayout) {
            TextInputLayout textInputLayout = (TextInputLayout) parent;
            textInputLayout.setError(msg);
            editText.requestFocus();
            editText.setSelection(textInputLayout.getEditText().length());
        }
    }
}
