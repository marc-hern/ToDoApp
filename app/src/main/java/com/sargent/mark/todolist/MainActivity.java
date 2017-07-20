package com.sargent.mark.todolist;

import android.view.View;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;

import android.view.MenuItem;
import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;


import com.sargent.mark.todolist.data.Contract;
import com.sargent.mark.todolist.data.DBHelper;

public class MainActivity extends AppCompatActivity implements AddToDoFragment.OnDialogCloseListener, UpdateToDoFragment.OnUpdateDialogCloseListener{

    private RecyclerView rv;
    private FloatingActionButton button;
    private DBHelper helper;
    private Cursor cursor;
    private SQLiteDatabase db;
    ToDoListAdapter adapter;
    private final String TAG = "mainactivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "oncreate called in main activity");
        button = (FloatingActionButton) findViewById(R.id.addToDo);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fm = getSupportFragmentManager();
                AddToDoFragment frag = new AddToDoFragment();
                frag.show(fm, "addtodofragment");
            }
        });
        rv = (RecyclerView) findViewById(R.id.recyclerView);
        rv.setLayoutManager(new LinearLayoutManager(this));


    }

    @Override
    protected void onStop() {
        super.onStop();
        if (db != null) db.close();
        if (cursor != null) cursor.close();
    }

    @Override
    protected void onStart() {
        super.onStart();

        helper = new DBHelper(this);
        db = helper.getWritableDatabase();
        cursor = getAllItems(db);

        adapter = new ToDoListAdapter(cursor, new ToDoListAdapter.ItemClickListener() {
            // Pass in category to update with corrrect category when clicked
            @Override
            public void onItemClick(int pos, String description, String duedate, long id, String category) {
                Log.d(TAG, "item click id: " + id);

                String[] dateInfo = duedate.split("-");

                int year = Integer.parseInt(dateInfo[0].replaceAll("\\s",""));
                int month = Integer.parseInt(dateInfo[1].replaceAll("\\s",""));
                int day = Integer.parseInt(dateInfo[2].replaceAll("\\s",""));

                FragmentManager fm = getSupportFragmentManager();

                UpdateToDoFragment frag = UpdateToDoFragment.newInstance(year, month, day, description, id, category);
                frag.show(fm, "updatetodofragment");
            }
        }, db);

        rv.setAdapter(adapter);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                long id = (long) viewHolder.itemView.getTag();
                Log.d(TAG, "passing id: " + id);
                removeToDo(db, id);

            }
        }).attachToRecyclerView(rv);
    }

    // Pass in category to closeDialog so categories can actually be added to the database
    @Override
    public void closeDialog(int year, int month, int day, String description, String category) {


        // Added category variable to end of list
        addToDo(db, description, formatDate(year, month, day), category);
        cursor = getAllItems(db);
        adapter.swapCursor(cursor);
    }

    public String formatDate(int year, int month, int day) {
        return String.format("%04d-%02d-%02d", year, month + 1, day);
    }



    private Cursor getAllItems(SQLiteDatabase db) {
        return db.query(
                Contract.TABLE_TODO.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                Contract.TABLE_TODO.COLUMN_NAME_DUE_DATE
        );
    }


    private long addToDo(SQLiteDatabase db, String description, String duedate, String category) {
        ContentValues cv = new ContentValues();
        cv.put(Contract.TABLE_TODO.COLUMN_NAME_DESCRIPTION, description);
        cv.put(Contract.TABLE_TODO.COLUMN_NAME_DUE_DATE, duedate);

        // Added category and eventcompleted fields to the database
        cv.put(Contract.TABLE_TODO.COLUMN_NAME_CATEGORY, category);
        cv.put(Contract.TABLE_TODO.COLUMN_NAME_EVENTCOMPLETED, 0);
        return db.insert(Contract.TABLE_TODO.TABLE_NAME, null, cv);
    }

    private boolean removeToDo(SQLiteDatabase db, long id) {
        Log.d(TAG, "deleting id: " + id);
        return db.delete(Contract.TABLE_TODO.TABLE_NAME, Contract.TABLE_TODO._ID + "=" + id, null) > 0;
    }

    // Updatetodo now accepts category as a parameter
    private int updateToDo(SQLiteDatabase db, int year, int month, int day, String description, long id, String category){

        String duedate = formatDate(year, month - 1, day);

        ContentValues cv = new ContentValues();
        cv.put(Contract.TABLE_TODO.COLUMN_NAME_DESCRIPTION, description);
        cv.put(Contract.TABLE_TODO.COLUMN_NAME_DUE_DATE, duedate);

        // Added category
        cv.put(Contract.TABLE_TODO.COLUMN_NAME_CATEGORY, category);

        return db.update(Contract.TABLE_TODO.TABLE_NAME, cv, Contract.TABLE_TODO._ID + "=" + id, null);
    }

    @Override
    public void closeUpdateDialog(int year, int month, int day, String description, long id, String category) {
        // Update the category of an event when changes are made
        updateToDo(db, year, month, day, description, id, category);
        adapter.swapCursor(getAllItems(db));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Inflates a menu for the spinner with categories
        getMenuInflater().inflate(R.menu.categories, menu);
        return true;
    }

    /*
    * ADDED NEW function to override the onOptionsItemSelected to handle when a menu item
    * is clicked. Depending on which is clicked we display the correct category.
    * */

    //Overrides onOptionsItemSelected to populate database
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if(itemId == R.id.all){
            cursor = getAllItems(db);
            adapter.swapCursor(cursor);
        }
        else if(itemId == R.id.homework){
            cursor = getSpecificItems(db, "Home");
            adapter.swapCursor(cursor);
        }
        else if(itemId == R.id.project){
            cursor = getSpecificItems(db, "Work");
            adapter.swapCursor(cursor);
        }
        else if(itemId == R.id.personal){
            cursor = getSpecificItems(db, "School");
            adapter.swapCursor(cursor);
        }
        else if(itemId == R.id.portfolio){
            cursor = getSpecificItems(db, "Extra");
            adapter.swapCursor(cursor);
        }

        return true;
    }
    //Find specific events
    private Cursor getSpecificItems(SQLiteDatabase db, String category) {

        return db.query(
                Contract.TABLE_TODO.TABLE_NAME,
                null,
                "category = '" + category + "'",
                null,
                null,
                null,
                Contract.TABLE_TODO.COLUMN_NAME_DUE_DATE
        );

    }
}
