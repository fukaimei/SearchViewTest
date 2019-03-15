package net.fkm.searchviewtest;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import net.fkm.searchviewbar.SearchFragment;
import net.fkm.searchviewbar.custom.IOnSearchClickListener;


public class MainActivity extends AppCompatActivity implements IOnSearchClickListener, Toolbar.OnMenuItemClickListener {

    private SearchFragment searchFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("搜索栏");
        setSupportActionBar(toolbar);
        searchFragment = SearchFragment.newInstance();
        toolbar.setOnMenuItemClickListener(this);
        searchFragment.setOnSearchClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.action_search:
                searchFragment.showFragment(getSupportFragmentManager(), SearchFragment.TAG);
                break;
        }
        return true;
    }

    @Override
    public void OnSearchClick(String keyword) {
        Toast.makeText(MainActivity.this, keyword, Toast.LENGTH_LONG).show();
    }
}
