package com.droidkit.sample.listenginetest;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.droidkit.core.Logger;
import com.droidkit.core.Utils;
import com.droidkit.engine.event.Events;
import com.droidkit.engine.event.NotificationCenter;
import com.droidkit.engine.event.NotificationListener;
import com.droidkit.engine.list.ListEngine;
import com.droidkit.engine.list.ListEngineClassConnector;
import com.droidkit.engine.list.ListEngineItem;
import com.droidkit.engine.list.ListEngineItemSerializator;
import com.droidkit.engine.list.adapter.SingleListSingleTableDataAdapter;
import com.droidkit.engine.list.sqlite.DbProvider;
import com.droidkit.sample.BaseActivity;
import com.droidkit.util.SafeRunnable;
import com.droidkit.sample.view.BlockingListView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;

import static com.droidkit.sample.listenginetest.TestProto.DialogTest;

public class TestListEngineActivity extends BaseActivity {

    private final static int PAGE_SIZE = 20;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private int padding;
    private BlockingListView lv;
    private ExampleAdapter adapter;

    private ListEngine<DialogTest> listEngine;

    private long count = 0;
    private long time = System.currentTimeMillis();

    private NotificationListener diskLoadListener;

    private volatile boolean isFirstLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initUi();

        final long start = SystemClock.uptimeMillis();

        diskLoadListener = new NotificationListener() {
            @Override
            public void onNotification(int eventType, int eventId, Object[] eventArgs) {
                if (isFirstLoad) {
                    isFirstLoad = false;
                    final long stop = SystemClock.uptimeMillis();
                    handler.postDelayed(new SafeRunnable() {
                        @Override
                        public void runSafely() {
                            Utils.showToast(TestListEngineActivity.this, "First data loaded in " + (stop - start) + "ms");
                        }
                    }, 3000);
                }

                if (adapter != null && lv != null) {
                    adapter.notifyDataSetChanged();
                    lv.setBlockLayoutChildren(false);
                }
            }
        };

        DbProvider.getDatabase(this, new DbProvider.DatabaseCallback() {

            @Override
            public void onDatabaseOpened(final SQLiteDatabase session) {

                final ListEngineClassConnector<DialogTest> classConnector = new ListEngineClassConnector<DialogTest>() {
                    @Override
                    public long getId(DialogTest value) {
                        return value.getId();
                    }

                    @Override
                    public long getSortKey(DialogTest value) {
                        return value.getTime();
                    }
                };

                final ListEngineItemSerializator<DialogTest> serializator = new ListEngineItemSerializator<DialogTest>() {
                    @Override
                    public ListEngineItem serialize(DialogTest entity) {
                        return new ListEngineItem(entity.getId(), entity.getTime(), ((DialogTest) entity).toByteArray());
                    }

                    @Override
                    public DialogTest deserialize(ListEngineItem item) {
                        try {
                            return DialogTest.parseFrom(item.data);
                        } catch (Exception e) {
                            Logger.d("tmp", "", e);
                            return null;
                        }
                    }
                };

                runOnUiThread(new SafeRunnable() {
                    @Override
                    public void runSafely() {
                        listEngine = new ListEngine<DialogTest>(TestListEngineActivity.this, new Comparator<DialogTest>() {
                            @Override
                            public int compare(DialogTest lhs, DialogTest rhs) {
                                int l = (int) (lhs.getTime() % Integer.MAX_VALUE);
                                int r = (int) (rhs.getTime() % Integer.MAX_VALUE);
                                return r - l;
                            }
                        }, new SingleListSingleTableDataAdapter(session, "DIALOG", false, serializator),
                                serializator,
                                classConnector
                        );
                        count = 0;
//                        listEngine.getLastKeyInDb(new KeyCallback<Long>() {
//                            @Override
//                            public void key(Long key) {
//                                if (key != null) {
//                                    count = key.longValue();
//                                } else {
//                                    count = 0;
//                                }
//                            }
//                        });
                        adapter = new ExampleAdapter();
                        lv.setAdapter(adapter);
                        NotificationCenter.getInstance().addListener(Events.LIST_ENGINE_UI_LIST_UPDATE, listEngine.getUniqueId(), diskLoadListener);
                        listEngine.loadNextListSlice(PAGE_SIZE);
                    }
                });
            }
        });

    }

    private void initUi() {
        final LinearLayout ll = new LinearLayout(this);
        ll.setBackgroundColor(Color.WHITE);
        ll.setOrientation(LinearLayout.VERTICAL);

        final LinearLayout controlLayout = new LinearLayout(this);
        controlLayout.setOrientation(LinearLayout.HORIZONTAL);

        final Button clearAll = new Button(this);
        clearAll.setText("Clear all");
        clearAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(listEngine != null) {
                    listEngine.clear();
                    count = 0;
                }
            }
        });
        controlLayout.addView(clearAll, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.5f));

        final CheckBox checkBox = new CheckBox(this);
        checkBox.setText("Bulk update");
        checkBox.setTextSize(12);
        checkBox.setChecked(true);
        controlLayout.addView(checkBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.5f));

        final EditText numberToAdd = new EditText(this);
        numberToAdd.setHint("Number to add...");
        numberToAdd.setInputType(InputType.TYPE_CLASS_NUMBER);
        numberToAdd.setText("1");
        numberToAdd.setTextSize(12);
        controlLayout.addView(numberToAdd, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.5f));

        final Button add = new Button(this);
        add.setText("Add");
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(listEngine != null) {
                    try {
                        final int intNumberToAdd = Integer.parseInt(numberToAdd.getText().toString());

                        if (checkBox.isChecked()) {
                            new AsyncTask<Void, Void, Void>() {
                                @Override
                                protected Void doInBackground(Void... params) {
                                    ArrayList<DialogTest> tmp = new ArrayList<DialogTest>();
                                    updateTime();
                                    for(int i = 0; i < intNumberToAdd; ++i) {
                                        count++;
                                        final DialogTest example = DialogTest.newBuilder().
                                                setId(count).setTime(++time).setTitle("Example Title").setText("Example Text").setStatus(1).setImageUrl("Image Url").setLastMessageSenderName("Example Name").build();
                                        tmp.add(example);
                                    }
                                    listEngine.addItems(tmp);
                                    return null;
                                }
                            }.execute();
                        } else {
                            new AsyncTask<Void, Void, Void>() {
                                @Override
                                protected Void doInBackground(Void... params) {
                                    updateTime();
                                    for (int i = 0; i < intNumberToAdd; ++i) {
                                        count++;
                                        final DialogTest example = DialogTest.newBuilder().
                                                setId(count).setTime(++time).setTitle("Example Title").setText("Example Text").setStatus(1).setImageUrl("Image Url").setLastMessageSenderName("Example Name").build();
                                        try {
                                            listEngine.addItem(example);
                                        } catch (Exception e) {
                                            Logger.d("tmp", "", e);
                                        }
                                    }
                                    return null;
                                }
                            }.execute();
                        }

                    } catch (Exception e) {
                        Utils.showToast(TestListEngineActivity.this, "Error parsing number");
                    }
                }
            }
        });
        controlLayout.addView(add, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.5f));

        ll.addView(controlLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.dpToPx(TestListEngineActivity.this, 50)));

        padding = Utils.dpToPx(this, 10);
        lv = new BlockingListView(this);
        lv.setBackgroundColor(Color.WHITE);
        lv.setCacheColorHint(Color.WHITE);
        lv.setScrollingCacheEnabled(false);
        ll.addView(lv);

        lv.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if(firstVisibleItem + visibleItemCount >= totalItemCount - 5 && listEngine != null) {
                    lv.setBlockLayoutChildren(true);
                    listEngine.loadNextListSlice(PAGE_SIZE);
                }
            }
        });

        setContentView(ll);
    }

    public class ExampleAdapter extends BaseAdapter {

        private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yy'-'HH:mm:ss");

        private final Drawable placeholder = new ColorDrawable(0xff999999);

        @Override
        public int getCount() {
            return listEngine.inMemoryListSize();
        }

        @Override
        public DialogTest getItem(int position) {
            return listEngine.getValueFromMemoryList(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @SuppressWarnings("deprecation")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            ViewHolder vh;

            if(v == null) {
                final Context self = TestListEngineActivity.this;

                FrameLayout fl = new FrameLayout(self);
                fl.setPadding(padding, padding, padding, padding);

                View avatar = new View(self);
                avatar.setBackgroundDrawable(placeholder);
                fl.addView(avatar, new FrameLayout.LayoutParams(Utils.dpToPx(TestListEngineActivity.this, 50), Utils.dpToPx(TestListEngineActivity.this, 50)));

                final TextView tv = new TextView(self);
                tv.setTextColor(Color.BLACK);
                tv.setPadding(Utils.dpToPx(TestListEngineActivity.this, 60), 0, 0, 0);
                tv.setTextSize(14);
                fl.addView(tv);
                vh = new ViewHolder();
                vh.title = tv;
                v = fl;
                v.setTag(vh);
            } else {
                vh = (ViewHolder) v.getTag();
            }

            DialogTest item = getItem(position);

            if(item != null) {
                vh.title.setText(
                        "ID: " + item.getId() + ", Title: " + item.getTitle() +
                                "\nCreate time: " + simpleDateFormat.format(item.getTime()) +
                                "\nText: " + item.getText()
                );
            }

            return v;
        }

        class ViewHolder {
            TextView title;
        }
    }

    private void updateTime() {
        time = Math.max(time, System.currentTimeMillis());
    }

}
