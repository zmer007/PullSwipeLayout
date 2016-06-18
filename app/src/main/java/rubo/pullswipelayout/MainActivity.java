package rubo.pullswipelayout;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;

import rubo.pullswipelayout.PullSwipeLayout.OnRefreshListener;

public class MainActivity extends AppCompatActivity {

    RecyclerView rv;
    PullSwipeLayout psl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        psl = (PullSwipeLayout) findViewById(R.id.mainPullSwipeLayout);
        psl.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh() {
                Toast.makeText(MainActivity.this, "refresh", Toast.LENGTH_SHORT).show();
            }
        });

        rv = (RecyclerView) findViewById(R.id.mainRecyclerView);
        assert rv != null;
        rv.setHasFixedSize(true);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new MyRecyclerAdapter());


    }

    public void change(View view) {
        psl.setRefreshing(!psl.isRefreshing());
    }
}
