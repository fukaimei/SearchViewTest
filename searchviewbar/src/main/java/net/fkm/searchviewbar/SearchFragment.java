package net.fkm.searchviewbar;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.fkm.searchviewbar.adapter.SearchHistoryAdapter;
import net.fkm.searchviewbar.custom.CircularRevealAnim;
import net.fkm.searchviewbar.custom.IOnItemClickListener;
import net.fkm.searchviewbar.custom.IOnSearchClickListener;
import net.fkm.searchviewbar.db.SearchHistoryDB;
import net.fkm.searchviewbar.utils.KeyBoardUtils;

import java.util.ArrayList;


public class SearchFragment extends DialogFragment implements DialogInterface.OnKeyListener,
        ViewTreeObserver.OnPreDrawListener, CircularRevealAnim.AnimListener,
        IOnItemClickListener, View.OnClickListener {

    public static final String TAG = "SearchFragment";
    private ImageView ivSearchBack;
    private EditText etSearchKeyword;
    private ImageView ivSearchSearch;
    private RecyclerView rvSearchHistory;
    private View searchUnderline;
    private TextView tvSearchClean;
    private View viewSearchOutside;

    private View view;
    //动画
    private CircularRevealAnim mCircularRevealAnim;
    //历史搜索记录
    private ArrayList<String> allHistorys = new ArrayList<>();
    private ArrayList<String> historys = new ArrayList<>();
    //适配器
    private SearchHistoryAdapter searchHistoryAdapter;
    //数据库
    private SearchHistoryDB searchHistoryDB;

    public static SearchFragment newInstance() {
        return new SearchFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.DialogStyle);
    }

    @Override
    public void onStart() {
        super.onStart();
        initDialog();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.dialog_search, container, false);

        init();//实例化

        return view;
    }

    private void init() {
        ivSearchBack = view.findViewById(R.id.iv_search_back);
        etSearchKeyword = view.findViewById(R.id.et_search_keyword);
        ivSearchSearch = view.findViewById(R.id.iv_search_search);
        rvSearchHistory = view.findViewById(R.id.rv_search_history);
        searchUnderline = view.findViewById(R.id.search_underline);
        tvSearchClean = view.findViewById(R.id.tv_search_clean);
        viewSearchOutside = view.findViewById(R.id.view_search_outside);

        //实例化动画效果
        mCircularRevealAnim = new CircularRevealAnim();
        //监听动画
        mCircularRevealAnim.setAnimListener(this);

        getDialog().setOnKeyListener(this);//键盘按键监听
        ivSearchSearch.getViewTreeObserver().addOnPreDrawListener(this);//绘制监听

        //实例化数据库
        searchHistoryDB = new SearchHistoryDB(getContext(), SearchHistoryDB.DB_NAME, null, 1);

        allHistorys = searchHistoryDB.queryAllHistory();
        setAllHistorys();
        //初始化recyclerView
        rvSearchHistory.setLayoutManager(new LinearLayoutManager(getContext()));//list类型
        searchHistoryAdapter = new SearchHistoryAdapter(getContext(), historys);
        rvSearchHistory.setAdapter(searchHistoryAdapter);

        //设置删除单个记录的监听
        searchHistoryAdapter.setOnItemClickListener(this);
        //监听编辑框文字改变
        etSearchKeyword.addTextChangedListener(new TextWatcherImpl());
        //监听点击
        ivSearchBack.setOnClickListener(this);
        viewSearchOutside.setOnClickListener(this);
        ivSearchSearch.setOnClickListener(this);
        tvSearchClean.setOnClickListener(this);

    }

    /**
     * 显示Fragment，防止多次打开导致崩溃
     */
    public void showFragment(FragmentManager fragmentManager, String tag) {
        if (!this.isAdded()) {
            this.show(fragmentManager, tag);
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.iv_search_back || view.getId() == R.id.view_search_outside) {
            hideAnim();
        } else if (view.getId() == R.id.iv_search_search) {
            search();
        } else if (view.getId() == R.id.tv_search_clean) {
            searchHistoryDB.deleteAllHistory();
            historys.clear();
            searchUnderline.setVisibility(View.GONE);
            searchHistoryAdapter.notifyDataSetChanged();
        }
    }

    /**
     * 初始化SearchFragment
     */
    private void initDialog() {
        Window window = getDialog().getWindow();
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int width = (int) (metrics.widthPixels * 0.98); //DialogSearch的宽
        window.setLayout(width, WindowManager.LayoutParams.MATCH_PARENT);
        window.setGravity(Gravity.TOP);
        window.setWindowAnimations(R.style.DialogEmptyAnimation);//取消过渡动画 , 使DialogSearch的出现更加平滑
    }

    /**
     * 监听键盘按键
     */
    @Override
    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            hideAnim();
        } else if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
            search();
        }
        return false;
    }

    /**
     * 监听搜索键绘制时
     */
    @Override
    public boolean onPreDraw() {
        ivSearchSearch.getViewTreeObserver().removeOnPreDrawListener(this);
        mCircularRevealAnim.show(ivSearchSearch, view);
        return true;
    }

    /**
     * 搜索框动画隐藏完毕时调用
     */
    @Override
    public void onHideAnimationEnd() {
        etSearchKeyword.setText("");
        dismiss();
    }

    /**
     * 搜索框动画显示完毕时调用
     */
    @Override
    public void onShowAnimationEnd() {
        if (isVisible()) {
            KeyBoardUtils.openKeyboard(getContext(), etSearchKeyword);
        }
    }

    /**
     * 监听编辑框文字改变
     */
    private class TextWatcherImpl implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            String keyword = editable.toString();
            if (TextUtils.isEmpty(keyword.trim())) {
                setAllHistorys();
                searchHistoryAdapter.notifyDataSetChanged();
            } else {
                setKeyWordHistorys(editable.toString());
            }
        }
    }

    /**
     * 点击单个搜索记录
     */
    @Override
    public void onItemClick(String keyword) {
        iOnSearchClickListener.OnSearchClick(keyword);
        hideAnim();
    }

    /**
     * 删除单个搜索记录
     */
    @Override
    public void onItemDeleteClick(String keyword) {
        searchHistoryDB.deleteHistory(keyword);
        historys.remove(keyword);
        checkHistorySize();
        searchHistoryAdapter.notifyDataSetChanged();
    }

    private void hideAnim() {
        KeyBoardUtils.closeKeyboard(getContext(), etSearchKeyword);
        mCircularRevealAnim.hide(ivSearchSearch, view);
    }

    private void search() {
        String searchKey = etSearchKeyword.getText().toString();
        if (TextUtils.isEmpty(searchKey.trim())) {
            Toast.makeText(getContext(), "请输入关键字", Toast.LENGTH_SHORT).show();
        } else {
            iOnSearchClickListener.OnSearchClick(searchKey);//接口回调
            searchHistoryDB.insertHistory(searchKey);//插入到数据库
            hideAnim();
        }
    }

    private void checkHistorySize() {
        if (historys.size() < 1) {
            searchUnderline.setVisibility(View.GONE);
        } else {
            searchUnderline.setVisibility(View.VISIBLE);
        }
    }

    private void setAllHistorys() {
        historys.clear();
        historys.addAll(allHistorys);
        checkHistorySize();
    }

    private void setKeyWordHistorys(String keyword) {
        historys.clear();
        for (String string : allHistorys) {
            if (string.contains(keyword)) {
                historys.add(string);
            }
        }
        searchHistoryAdapter.notifyDataSetChanged();
        checkHistorySize();
    }

    private IOnSearchClickListener iOnSearchClickListener;

    public void setOnSearchClickListener(IOnSearchClickListener iOnSearchClickListener) {
        this.iOnSearchClickListener = iOnSearchClickListener;
    }

}
